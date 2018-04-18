// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import de.heikoseeberger.sbtheader.{CommentStyle, FileType}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderFileType

import net.cakesolutions.CakePlatformDependencies._

// Copy centralised library dependencies into this build level
Compile / unmanagedSources +=
  baseDirectory.value / "project" / "project" / "CakePlatformDependencies.scala"

organization := "net.cakesolutions"

name := "sbt-cake"
sbtPlugin := true

// scalastyle:off magic.number
startYear := Some(2017)
licenses :=
  Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
headerLicense := Some(
  HeaderLicense.Custom(
    """|Copyright: 2017-2018 https://github.com/cakesolutions/sbt-cake/graphs
       |License: http://www.apache.org/licenses/LICENSE-2.0
       |""".stripMargin
  )
)
headerMappings :=
  headerMappings.value ++
  Map(
    FileType("sbt") -> CommentStyle.cppStyleLineComment,
    HeaderFileType.java -> CommentStyle.cppStyleLineComment,
    HeaderFileType.scala -> CommentStyle.cppStyleLineComment
  )

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin(SbtDependencies.buildInfo)
addSbtPlugin(SbtDependencies.pgp)
addSbtPlugin(SbtDependencies.digest)
addSbtPlugin(SbtDependencies.git)
addSbtPlugin(SbtDependencies.gzip)
addSbtPlugin(SbtDependencies.packager)
addSbtPlugin(SbtDependencies.Coursier.sbt)
addSbtPlugin(SbtDependencies.scoverage)
addSbtPlugin(SbtDependencies.scalafix)

addSbtPlugin(SbtDependencies.gatling)

// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin(SbtDependencies.scalafmt)

enablePlugins(ScalafmtPlugin)

dependencyOverrides ++= Seq(
  guava,
  typesafeConfig,
  SbtDependencies.Coursier.cache,
  SbtDependencies.Coursier.core,
  SbtDependencies.packager,
  ApacheCommons.codec,
  ApacheCommons.logging,
  ApacheCommons.compress,
  ApacheCommons.lang3,
  httpClient,
  levelDbJni,
  quasiQuotes,
  Slf4j.api,
  Webjars.locator
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

scriptedBufferLog := false
scriptedLaunchOpts := Seq("-Dplugin.version=" + version.value)
