// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "2.0.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.2.0")

// To enable Sonatype publishing of this project's code
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")

// Scala style and formatting for this plugins code
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
// The following plugin is otherwise known as neo-sbt-scalafmt
// - see: https://github.com/lucidsoftware/neo-sbt-scalafmt
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.7")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
