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
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.3")

// WORKAROUND https://github.com/scalameta/scalafmt/issues/925
//
// see ScalafmtPlugin.scala for more and do not use 0.6.8
libraryDependencies += "com.geirsson" %% "scalafmt-bootstrap" % "0.6.6"

scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts := Seq(
  "-Dplugin.version=" + version.value
)
