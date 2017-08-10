organization := "net.cakesolutions"

name := "sbt-cake"
sbtPlugin := true

sonatypeGithub := ("cakesolutions", "sbt-cake")
licenses := Seq(Apache2)

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.16")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.1")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC10")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.1.1")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.10")

scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts := Seq(
  "-Dplugin.version=" + version.value
)
