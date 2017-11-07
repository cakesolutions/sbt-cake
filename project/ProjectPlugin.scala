import net.cakesolutions._
import sbt.Keys._
import sbt._
import sbt.complete.{DefaultParsers, Parser}
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbtassembly.AssemblyKeys.assembly
import wartremover._

/** Example of how to apply settings across a build (difficult to do in build.sbt) */
object ProjectPlugin extends AutoPlugin {
  override def requires = CakePlatformPlugin

  override def trigger = allRequirements

  val autoImport = ProjectPluginKeys

  import autoImport._

  // NOTE: everything in here is applied once, to the entire build
  override val buildSettings = Seq(
    scalaVersion := "2.12.4",
    aggregate in dynverWrite := false,
    dynverFile := file("version"),
    dynverWrite := {
      val out = dynverFile.value
      val v = version.value
      IO.write(out, v)
    },
    name := "adengine",
    organization := "com.foo"
  ) ++ addCommandAlias(
    "fmt",
    ";sbt:scalafmt ;scalafmt ;test:scalafmt ;it:scalafmt ;fun:scalafmt ;resilience:scalafmt ;jmh:scalafmt"
  ) ++ addCommandAlias(
    "fmttest",
    ";sbt:scalafmt::test ;scalafmt::test ;test:scalafmt::test ;it:scalafmt::test ;fun:scalafmt::test ;resilience:scalafmt::test ;jmh:scalafmt::test"
  ) ++ addCommandAlias(
    "cpl",
    ";compile ;test:compile ;it:compile ;fun:compile ;resilience:compile ;jmh:compile"
  ) ++ addCommandAlias(
    // WORKAROUND https://github.com/sbt/sbt-native-packager/issues/974
    "dockerPublishLocalHack",
    List(
      ";rulemgmt/docker:publishLocal",
      ";wibbleAggregator/docker:publishLocal",
      ";thing/docker:publishLocal"
    ).mkString(" ")
  ) ++ addCommandAlias(
    // WORKAROUND https://github.com/sbt/sbt-native-packager/issues/974
    "dockerPublishHack",
    List(
      ";rulemgmt/docker:publish",
      ";wibbleAggregator/docker:publish",
      ";thing/docker:publish"
    ).mkString(" ")
  ) ++ addCommandAlias(
    "dockerTests",
    List(
      ";dockerRemove",
      ";dockerPublishLocalHack",
      ";debugging/it:dockerComposeTestQuick",
      ";cis/it:dockerComposeTestQuick",
      ";rulemgmt/it:dockerComposeTestQuick",
      ";rulemgmt/fun:dockerComposeTestQuick",
      ";wibbleQueue/it:dockerComposeTestQuick",
      ";wibbleAggregator/it:dockerComposeTestQuick",
      ";wibbleAggregator/fun:dockerComposeTestQuick",
      ";thing/it:dockerComposeTestQuick",
      ";thing/fun:dockerComposeTestQuick",
      ";thing/resilience:dockerComposeTestQuick",
      ";dockerRemove"
    ).mkString(" ")
  ) ++ addCommandAlias(
    "ci",
    ";fmttest ;cpl ;test ;dockerTests"
  )

  // NOTE: everything in here is applied to every project (a better `commonSettings`)
  override val projectSettings = Seq(
    // too many false positives from the Any wart.
    // DefaultArguments is not necessarilly a bad thing.
    wartremoverWarnings in (Compile, compile) --= Seq(
      Wart.Any,
      Wart.DefaultArguments,
      Wart.ExplicitImplicitTypes // https://github.com/fommil/stalactite/issues/7
    ),
    // some "bad style" is acceptable in tests, such as side-effecting
    // matchers, injecting nulls, inspecting state with vars and
    // stress testing exception handling.
    wartremoverWarnings in (Test, compile) :=
      (wartremoverWarnings in (Compile, compile)).value
        .filterNot(_ == Wart.NonUnitStatements)
        .filterNot(_ == Wart.Var)
        .filterNot(_ == Wart.Null)
        // DESNOTE(2017-07-18, PRodriguez): https://github.com/wartremover/wartremover/issues/385
        .filterNot(_ == Wart.OptionPartial)
        .filterNot(_ == Wart.Throw),
    moduleName := (name in ThisBuild).value + "-" + name.value,
    scalacOptions += "-language:higherKinds",
    scalacOptions += "-Ywarn-unused:imports,patvars,privates,locals,params,implicits",
    scalacOptions in (Compile, console) ~= (_ filterNot (_ contains "unused")),
    scalacOptions in (Compile, console) -= "-Xlint",
    initialCommands in (Compile, console) := Seq(
      "cats._",
      "cats.data._",
      "cats.implicits._",
      "shapeless.{ :: => :*:, _ }"
    ).mkString("import ", ",", ""),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch
    ),
    libraryDependencies ++= Seq(
      "com.github.mpilquist" %% "simulacrum" % "0.11.0",
      "com.fommil" %% "stalactite" % "0.0.5",
      "org.typelevel" %% "cats" % "0.9.0",
      "org.typelevel" %% "cats-effect" % "0.3",
      "org.typelevel" %% "kittens" % "1.0.0-M9",
      "com.chuusai" %% "shapeless" % "2.3.2"
    ),
    scalacOptions ++= {
      val dir = (baseDirectory in ThisBuild).value / "project"
      Seq(
        s"-Xmacro-settings:stalactite.targets=$dir/stalactite-targets.conf",
        s"-Xmacro-settings:stalactite.defaults=$dir/stalactite-defaults.conf"
      )
    },
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    fullResolvers -= DefaultMavenRepository
  )
}

object ProjectPluginKeys {
  // NOTE: anything in here is automatically visible in build.sbt
  val dynverFile = settingKey[File]("File used by dynverWrite")
  val dynverWrite = taskKey[Unit]("Writes out the version to a file")

  /**
    * ResilienceTest is designed to be run in a dockerised prod-like
    * environment with test fixtures being able to control the
    * availability of third party dependencies, and tests asserting
    * that graceful failure is attained.
    */
  val ResilienceTest = config("resilience") extend (Test)

  implicit class ResilienceTestOps(p: Project) {
    import com.typesafe.sbt.SbtNativePackager._
    import CakeBuildKeys._
    import CakeDockerComposePluginKeys._

    def enableResilienceTests: Project = {
      import CakeDockerComposePlugin._
      import CakeDockerComposePluginKeys._
      p.configs(ResilienceTest)
        .enablePlugins(CakeDockerComposePlugin)
        .settings(
          inConfig(ResilienceTest)(
            Defaults.testSettings ++
              sensibleTestSettings ++
              scalafmtSettings ++
              dockerComposeSettings ++
              Seq(
                wartremoverWarnings in compile := (wartremoverWarnings in (Test, compile)).value,
                dockerComposeImages := (publishLocal in Docker).value,
                dockerComposeFile := (dockerComposeFile in ResilienceTest).value,
                dockerComposeTestQuick := dockerComposeTestQuickTask.value,
                javaOptions += s"-Ddocker.compose.file=${dockerComposeFile.value.getCanonicalPath}"
              )
          )
        )
    }
  }

  case class GatlingConfig(start: Int,
                           finish: Int,
                           minutes: Int,
                           name: String,
                           hostport: String)
  def gatlingParser: Parser[GatlingConfig] = {
    import DefaultParsers._

    (Space ~ NatBasic ~ Space ~ NatBasic ~ Space ~ NatBasic ~ Space ~ StringBasic ~ Space.? ~ StringBasic.?)
      .map {
        case _ ~ start ~ _ ~ finish ~ _ ~ minutes ~ _ ~ name ~ _ ~ hostport
            if finish >= start =>
          GatlingConfig(
            start,
            finish,
            minutes,
            name,
            hostport.getOrElse("localhost:9010")
          )
      }
  }

  val gatlingTask = {
    val parser = (s: State) => gatlingParser
    Def.inputTask {
      val GatlingConfig(start, finish, mins, clazz, hostport) = parser.parsed
      val data = (target.value / "data")
      val results = (target.value / "results")
      data.mkdirs()
      results.mkdirs()
      val props = Seq(
        s"-DstartWithUsersPerSec=$start",
        s"-Dgatling.core.directory.data=$data",
        s"-Dgatling.core.directory.results=$results",
        s"-DfinishWithUsersPerSec=$finish",
        s"-DrampUpDurationMins=$mins",
        s"-DtargetEndpoint=$hostport",
        s"-Dreports=true",
        s"-DsimulationClass=foo.$clazz"
      )
      val log = streams.value.log
      val options = ForkOptions(
        javaHome = javaHome.value,
        outputStrategy = outputStrategy.value,
        bootJars = Vector.empty[java.io.File],
        workingDirectory = Option(baseDirectory.value),
        runJVMOptions = ((javaOptions in Compile).value ++ props).toVector,
        connectInput = connectInput.value,
        envVars = (envVars in Compile).value
      )
      new ForkRun(options)
        .run(
          (mainClass in assembly).value.get,
          Attributed.data((fullClasspath in Compile).value),
          Nil,
          log
        )
        .failed
        .foreach(f => sys.error(f.getMessage))

      log.info(s"Summary is in $results")
    }
  }
}
