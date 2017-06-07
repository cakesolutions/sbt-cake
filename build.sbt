// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

organization := "net.cakesolutions"

name := "sbt-cake"
sbtPlugin := true

sonatypeGithub := ("cakesolutions", "sbt-cake")
licenses := Seq(Apache2)

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.15")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.6")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.1.0")

// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.3")
// The following is based on: http://scalameta.org/scalafmt/#sbt0.13
// WORKAROUND https://github.com/scalameta/scalafmt/issues/925
//
// see ScalafmtPlugin.scala for more and do not use 0.6.8
libraryDependencies += "com.geirsson" %% "scalafmt-bootstrap" % "0.6.6"

// TODO: CO-68: remove JSR305 dependency when SBT moves away from Scala 2.10
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2" % "provided"

scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts := Seq(
  "-Dplugin.version=" + version.value
)
