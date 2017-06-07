// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("com.fommil" % "sbt-sensible" % "1.2.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "2.0.0")

// Scala style and formatting for this plugins code
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
// TODO: CO-77: load scalafmt plugin
// addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.4")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
