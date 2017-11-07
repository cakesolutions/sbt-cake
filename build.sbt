import sbt.InputKey
// Please read https://github.com/cakesolutions/sbt-cake
//
// NOTE: build-wide settings are in project/ProjectPlugin.scala

// TODO: ADENG-197 Dependency Management

val playVersion = play.core.PlayVersion.current
def host = sys.env.get("DOCKER_HOST_IP").getOrElse("localhost")

lazy val thing =
  project.enablePlay.enableIntegrationTests.enableFunctionalTests.enableResilienceTests
    .enablePlugins(
      BuildInfoPlugin,
      DockerPlugin
    )
    .settings(
      // play routes file has a lot of noise in it...
      scalacOptions in Compile -= "-Ywarn-unused:imports,patvars,privates,locals,params,implicits",
      scalacOptions in Compile -= "-Xlint",
      scalacOptions in Compile += "-Xlint:-unused,_",
      pipelineStages := Seq(digest, gzip),
      libraryDependencies ++= Seq(
        "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
        "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.6.3",
        "io.dropwizard.metrics" % "metrics-core" % "3.2.2",
        "io.dropwizard.metrics" % "metrics-jvm" % "3.2.2",
        "io.dropwizard.metrics" % "metrics-graphite" % "3.2.2",
        "com.gu" %% "scanamo" % "0.9.3",
        "io.prometheus" % "simpleclient" % "0.0.23",
        ws, // Play webserver client library
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "it,test",
        "commons-validator" % "commons-validator" % "1.4.0" % "test",
        "com.lihaoyi" %% "fastparse" % "1.0.0"
      ),
      parallelExecution in Test := false,
      parallelExecution in IntegrationTest := false,
      parallelExecution in FunctionalTest := false,
      javaOptions in IntegrationTest += "-Dcom.amazonaws.sdk.disableCertChecking=true",
      javaOptions in FunctionalTest += "-Dcom.amazonaws.sdk.disableCertChecking=true",
      javaOptions in ResilienceTest += "-Dcom.amazonaws.sdk.disableCertChecking=true",
      envVars in IntegrationTest += ("AWS_CBOR_DISABLE" -> "true"),
      envVars in FunctionalTest += ("AWS_CBOR_DISABLE" -> "true"),
      envVars in ResilienceTest += ("AWS_CBOR_DISABLE" -> "true")
    )
    .settings(
      inConfig(FunctionalTest)(
        dockerComposeImages := {
          val _ = dockerComposeImages.value
          (publishLocal in Docker in wibbleAggregator).value
        }
      ),
      inConfig(ResilienceTest)(
        dockerComposeImages := {
          val _ = dockerComposeImages.value
          (publishLocal in Docker in wibbleAggregator).value
        }
      )
    )
    .dependsOn(
      debugging,
      cis,
      cis % "test->test",
      wibbleQueue,
      playlogging,
      common,
      events % "compile->compile;test->test",
      kinesis,
      redis,
      commontest % "test->compile;it->compile;fun->compile;resilience->compile",
      csvformat,
      dynamo
    )

lazy val rulemgmt =
  project.enablePlay.enableIntegrationTests.enableFunctionalTests
    .enablePlugins(
      BuildInfoPlugin,
      DockerPlugin
    )
    .settings(
      // play routes file has a lot of noise in it...
      scalacOptions in Compile -= "-Ywarn-unused:imports,patvars,privates,locals,params,implicits",
      scalacOptions in Compile -= "-Xlint",
      scalacOptions in Compile += "-Xlint:-unused,_",
      pipelineStages := Seq(digest, gzip),
      libraryDependencies ++= Seq(
        "com.propensive" %% "contextual-examples" % "1.0.0",
        "io.dropwizard.metrics" % "metrics-core" % "3.2.5",
        "io.dropwizard.metrics" % "metrics-jvm" % "3.2.5",
        "io.dropwizard.metrics" % "metrics-graphite" % "3.2.5",
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "it,test",
        ws % "it,test" // Play webserver client library
      )
    )
    .dependsOn(
      debugging,
      playlogging,
      common,
      commontest % "test->compile;it->compile"
    )

lazy val akkaHttpVersion = "10.0.10"

lazy val wibbleAggregator =
  project
    .enablePlugins(JavaAppPackaging, BuildInfoPlugin, DockerPlugin)
    .enableIntegrationTests
    .enableFunctionalTests
    .settings(
      mainClass := Some("foo.Main"),
      libraryDependencies ++= Seq(
        ws,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
        "de.heikoseeberger" %% "akka-http-play-json" % "1.18.0",
        "com.gu" %% "scanamo" % "0.9.3",
        "com.github.blemale" %% "scaffeine" % "2.3.0" % "compile"
      ),
      packageName in Docker := "wibble-aggregator"
    )
    .dependsOn(
      retry,
      wibbleQueue,
      monitoring,
      cis,
      cis % "test->test",
      redis,
      dynamo,
      common,
      logging,
      commontest % "test->compile;it->compile"
    )

lazy val wibbleQueue =
  project.enableIntegrationTests
    .settings(
      libraryDependencies ++= Seq(
        "org.scodec" %% "scodec-core" % "1.10.3"
      )
    )
    .dependsOn(
      common,
      logging,
      cis,
      redis,
      commontest % "test->compile;it->compile"
    )

lazy val cis =
  project.enableIntegrationTests
    .settings(
      scalacOptions in Compile -= "-Ywarn-unused:imports,patvars,privates,locals,params,implicits",
      scalacOptions in Compile -= "-Xlint",
      scalacOptions in Compile += "-Xlint:-unused,_",
      libraryDependencies ++= Seq(
        "org.scodec" %% "scodec-core" % "1.10.3",
        "com.gu" %% "scanamo" % "0.9.3",
        "io.prometheus" % "simpleclient" % "0.0.23"
      )
    )
    .dependsOn(
      common,
      dynamo,
      logging,
      monitoring,
      commontest % "test->compile;it->compile"
    )

lazy val logging = project
  .settings(
    libraryDependencies ++= Seq(
      "com.propensive" %% "contextual" % "1.0.1",
      "com.github.pureconfig" %% "pureconfig" % "0.7.2"
    )
  )
  .dependsOn(macros, xmlformat, jsonformat)

lazy val macros = project

lazy val playlogging =
  project
    .settings(
      libraryDependencies ++= Seq(guice, openId),
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % playVersion,
        "io.prometheus" % "simpleclient" % "0.0.23",
        "io.prometheus" % "simpleclient_graphite_bridge" % "0.0.23",
        "io.prometheus" % "simpleclient_hotspot" % "0.0.23",
        "io.prometheus" % "simpleclient_dropwizard" % "0.0.23",
        "io.dropwizard.metrics" % "metrics-core" % "3.2.5",
        "io.dropwizard.metrics" % "metrics-jvm" % "3.2.5",
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test"
      )
    )
    .dependsOn(logging, common, serialization, monitoring, commontest % "test")

lazy val monitoring =
  project
    .settings(
      scalacOptions in Compile -= "-Ywarn-unused:imports,patvars,privates,locals,params,implicits",
      scalacOptions in Compile -= "-Xlint",
      scalacOptions in Compile += "-Xlint:-unused,_",
      libraryDependencies ++= List(
        "io.prometheus" % "simpleclient" % "0.0.23",
        "io.prometheus" % "simpleclient_dropwizard" % "0.0.23",
        "io.prometheus" % "simpleclient_hotspot" % "0.0.23",
        "io.prometheus" % "simpleclient_graphite_bridge" % "0.0.23",
        "io.dropwizard.metrics" % "metrics-jvm" % "3.2.5"
      )
    )
    .dependsOn(common, commontest % "test")

lazy val performance = project
  .settings(
    javaOptions ++= Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-Djava.net.preferIPv6Addresses=false"
    ),
    libraryDependencies ++= {
      val version = "2.3.0"
      Seq(
        "io.gatling" % "gatling-app" % version,
        "io.gatling.highcharts" % "gatling-charts-highcharts" % version,
        "io.gatling" % "gatling-test-framework" % version,
        "io.gatling" % "gatling-http" % version
      )
    },
    test in assembly := {},
    mainClass in assembly := Some("foo.EntryPoint"),
    artifact in (Compile, assembly) :=
      (artifact in (Compile, assembly)).value
        .withClassifier(Some("assembly")),
    assemblyMergeStrategy in assembly := {
      case "META-INF/io.netty.versions.properties" => MergeStrategy.first
      case x => (assemblyMergeStrategy in assembly).value(x)
    },
    addArtifact(artifact in (Compile, assembly), assembly),
    InputKey[Unit]("gatling", "start end minutes className") := gatlingTask.evaluated,
    TaskKey[Unit]("populateCis", "") := (runMain in Compile)
      .toTask(
        " foo.PopulateCIS"
      )
      .value
  )

lazy val retry = project

lazy val common = project
  .settings(
    libraryDependencies ++= Seq(
      "io.netty" % "netty-common" % "4.1.15.Final",
      "com.github.cb372" %% "scalacache-caffeine" % "0.9.4",
      "com.github.pureconfig" %% "pureconfig" % "0.7.2",
      "com.github.pureconfig" %% "pureconfig-akka" % "0.8.0",
      "com.amazonaws" % "aws-java-sdk" % "1.11.170"
    )
  )
  .dependsOn(logging)

// until contextual-examples 1.0.2+ is released
lazy val xpath = project
  .settings(
    libraryDependencies ++= Seq(
      "com.propensive" %% "contextual" % "1.0.1"
    )
  )

lazy val debugging = project.enableIntegrationTests
  .dependsOn(xpath, jsonformat, commontest % "test->test;it->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.mariadb.jdbc" % "mariadb-java-client" % "2.1.2",
      "org.tpolecat" %% "doobie-core-cats" % "0.4.4",
      "org.tpolecat" %% "doobie-hikari-cats" % "0.4.4",
      "org.tpolecat" %% "doobie-scalatest-cats" % "0.4.4" % "test,it"
    )
  )

lazy val rigidsearch = project
  .enablePlugins(SbtProguard)
  .settings(
    // https://www.guardsquare.com/en/proguard/manual/introduction
    // https://www.guardsquare.com/en/proguard/manual/examples
    javaOptions in (Proguard, proguard) := Seq("-Xmx5g"),
    proguardOptions in Proguard ++= Seq(
      "-dontnote",
      "-dontwarn",
      "-ignorewarnings",
      "-dontoptimize",
      "-dontobfuscate",
      // our entry method
      """-keep class foo.RigidSearch {
                 void handler(com.amazonaws.services.lambda.runtime.events.KinesisEvent,
                              com.amazonaws.services.lambda.runtime.Context);
               }""",
      // reflectively loaded database driver
      """-keep class org.mariadb.jdbc.Driver""",
      // it is not enough to request implementations of KinesisEvent /
      // Context, we must import their entire public API.
      """-keep public class com.amazonaws.services.lambda.runtime.** { public *; }""",
      // AWS reflectively loads DateTime
      """-keep class org.joda.time.DateTime""",
      // bugs in proguard's scala support... this is costing ~15MB
      """-keep class scala.** { *; }"""
    ),
    libraryDependencies ++= Seq(
      "com.amazonaws" % "amazon-kinesis-client" % "1.8.7",
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
      "com.amazonaws" % "aws-lambda-java-events" % "2.0.1"
    ),
    //test in assembly := {},
    TaskKey[Unit]("lambdaPublish", "") := {
      import scala.sys.process._
      //val jar = assembly.value
      val jar = (proguard in Proguard).value.head
      val name = jar.getName
      s"aws --profile=adtech s3 cp $jar s3://foo-lambda/$name".!
      IO.copyFile(jar, file(jar.getName))
    }
  )
  .dependsOn(debugging)

lazy val commontest = project
  .settings(
    libraryDependencies ++= Seq(
      "com.weightwatchers" %% "reactive-kinesis" % "0.6.0",
      "com.typesafe.play" %% "play" % playVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.7.2",
      "io.prometheus" % "simpleclient" % "0.0.23",
      "org.scalatest" %% "scalatest" % "3.0.1",
      ws
    )
  )
  .dependsOn(common)

lazy val monkeys = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.propensive" %% "contextual" % "1.0.1"
    )
  )

lazy val xmlformat = project
  .dependsOn(monkeys)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.benhutchison" %% "mouse" % "0.9",
      "org.scalactic" %% "scalactic" % "3.0.3",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
    )
  )

lazy val jsonformat = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion
    )
  )

lazy val events = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % "test"
    )
  )
  .dependsOn(kinesis, logging, serialization, commontest % "test")

lazy val csvformat = project
  .enablePlugins(NeoJmh)
  .settings(
    envVars in JmhInternal += ("RANDOM_DATA_GENERATOR_SEED" -> "-2806676364240068721"),
    libraryDependencies ++= Seq(
      "com.danielasfregola" %% "random-data-generator" % "2.2" % "test,jmh"
    )
  )

lazy val dynamo =
  project
    .settings(
      libraryDependencies ++= Seq(
        "com.gu" %% "scanamo" % "0.9.3"
      )
    )
    .dependsOn(common)

lazy val kinesis = project
  .settings(
    libraryDependencies ++= Seq(
      "com.weightwatchers" %% "reactive-kinesis" % "0.6.0"
    )
  )
  .dependsOn(logging, common, serialization)

lazy val redis = project
  .settings(
    libraryDependencies ++= Seq(
      "org.redisson" % "redisson" % "3.5.4"
    )
  )
  .dependsOn(logging, serialization, common)

lazy val serialization = project
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile += sourceDirectory.value / "test/protobuf",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion,
      "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )
  .dependsOn(logging, commontest % "test", csvformat)
