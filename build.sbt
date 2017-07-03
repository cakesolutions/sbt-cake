// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderFileType

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

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.15")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.6")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.1.0")
// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.7")

enablePlugins(ScalafmtPlugin)

// TODO: CO-68: remove JSR305 dependency when SBT moves away from Scala 2.10
val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.2"
libraryDependencies += findbugs % "provided"

dependencyOverrides ++= Set(
  findbugs,
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
