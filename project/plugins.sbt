// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakePlatformDependencies.SbtDependencies._

// Copy centralised library dependencies into this build level
unmanagedSources in Compile +=
  baseDirectory.value / "project" / "CakePlatformDependencies.scala"

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin(header)
addSbtPlugin(dynver)

// To enable Sonatype publishing of this project's code
addSbtPlugin(pgp)
addSbtPlugin(sonatype)

// Scala style and formatting for this plugins code
addSbtPlugin(scalastyle)
addSbtPlugin(scalafmt)

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
