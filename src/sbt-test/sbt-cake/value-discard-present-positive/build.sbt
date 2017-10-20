// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakePlatformDependencies._

name in ThisBuild := "value-discard-present-positive"

enablePlugins(CakeStandardsPlugin)

libraryDependencies ++= Seq(
  scalatest
)

// Ensure "-Ywarn-value-discard" is enabled for the test scope
scalacOptions in Test :=
  (scalacOptions in Test).value ++
    Seq("-Ywarn-value-discard", "-Xfatal-warnings")
