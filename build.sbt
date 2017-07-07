// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderFileType
import CakeDependencies._

organization := "net.cakesolutions"

// scalastyle:off magic.number

name := "sbt-cake"
sbtPlugin := true

startYear := Some(2017)
licenses :=
  Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
headerLicense := Some(
  HeaderLicense.Custom(
    """|Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
       |License: http://www.apache.org/licenses/LICENSE-2.0
       |""".stripMargin
  )
)
headerMappings :=
  headerMappings.value ++
  Map(
    FileType("sbt") -> HeaderCommentStyle.CppStyleLineComment,
    HeaderFileType.java -> HeaderCommentStyle.CppStyleLineComment,
    HeaderFileType.scala -> HeaderCommentStyle.CppStyleLineComment
  )

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

// TODO: CO-13: clean up this dependency maintenance nightmare

addSbtPlugin(SbtDependencies.dynver)
addSbtPlugin(SbtDependencies.buildInfo)
addSbtPlugin(SbtDependencies.pgp)
addSbtPlugin(SbtDependencies.plugin)
addSbtPlugin(SbtDependencies.digest)
addSbtPlugin(SbtDependencies.git)
addSbtPlugin(SbtDependencies.gzip)
addSbtPlugin(SbtDependencies.packager)
addSbtPlugin(SbtDependencies.coursier)
addSbtPlugin(SbtDependencies.scoverage)
addSbtPlugin(SbtDependencies.wartRemover)
// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin(SbtDependencies.scalafmt)

enablePlugins(ScalafmtPlugin)

// TODO: CO-68: remove JSR305 dependency when SBT moves away from Scala 2.10
libraryDependencies += jsr305 % "provided"

// These are dependency overrides which are different than the regular dependencies
// and should be maintained here.
// TODO: CO-143: Ideally we should refactor all dependencies in a single place.
dependencyOverrides ++= Set(
  jsr305,
  "com.google.guava" % "guava" % "19.0",
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.sbt" % "sbt-js-engine" % "1.1.4",
  "com.typesafe.sbt" % "sbt-native-packager" % "1.1.6",
  "com.typesafe.sbt" % "sbt-web" % "1.3.0",
  "commons-codec" % "commons-codec" % "1.6",
  "commons-logging" % "commons-logging" % "1.1.3",
  "org.apache.commons" % "commons-compress" % "1.9",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.fusesource.leveldbjni" % "leveldbjni" % "1.7",
  "org.scalamacros" %% "quasiquotes" % "2.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.webjars" % "webjars-locator" % "0.26"
)

// For publishing this plugin to Sonatype in CI environments
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ =>
  false
}
publishTo := {
  val nexus = "https://oss.sonatype.org/"

  if (sys.env.get("CI").isDefined) {
    if (isSnapshot.value) {
      Some("snapshots" at nexus + "content/repositories/snapshots")
    } else {
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  } else {
    None
  }
}
pomExtra := {
  val user = "cakesolutions"
  val repository = name.value

  <url>https://github.com/{user}/{repository}</url>
  <scm>
    <url>
      https://github.com/{user}/{repository}
    </url>
    <connection>
      https://github.com/{user}/{repository}
    </connection>
  </scm>
  <developers>
    <developer>
      <id>{user}</id>
    </developer>
  </developers>
}

scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts := Seq("-Dplugin.version=" + version.value)
