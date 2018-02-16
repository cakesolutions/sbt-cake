// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderFileType

import net.cakesolutions.CakePlatformDependencies._

// Copy centralised library dependencies into this build level
unmanagedSources in Compile +=
  baseDirectory.value / "project" / "project" / "CakePlatformDependencies.scala"

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

addSbtPlugin(SbtDependencies.buildInfo)
addSbtPlugin(SbtDependencies.pgp)
addSbtPlugin(SbtDependencies.digest)
addSbtPlugin(SbtDependencies.git)
addSbtPlugin(SbtDependencies.gzip)
addSbtPlugin(SbtDependencies.packager)
addSbtPlugin(SbtDependencies.Coursier.sbt)
addSbtPlugin(SbtDependencies.scoverage)
addSbtPlugin(SbtDependencies.wartRemover)

addSbtPlugin(SbtDependencies.gatling)

// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin(SbtDependencies.scalafmt)

enablePlugins(ScalafmtPlugin)

// TODO: CO-68: remove JSR305 dependency when SBT moves away from Scala 2.10
libraryDependencies += jsr305 % "provided"

dependencyOverrides ++= Set(
  jsr305,
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

scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts := Seq("-Dplugin.version=" + version.value)
