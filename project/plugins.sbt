// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakePlatformDependencies.SbtDependencies._

// An SBT source generator is used to copy centralised library dependencies
// into this build level
sourceGenerators in Compile += Def.task {
  val deps =
    (baseDirectory in Compile).value /
      "project" / "CakePlatformDependencies.scala"
  val projectDeps =
    (sourceManaged in Compile).value / "CakePlatformDependencies.scala"

  IO.copyFile(deps, projectDeps)

  Seq(projectDeps)
}.taskValue

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
