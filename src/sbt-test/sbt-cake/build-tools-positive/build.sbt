// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

name in ThisBuild := "build-tools-positive"

enablePlugins(CakeBuildInfoPlugin)

externalBuildTools :=
  Seq("ls" -> "`ls` should exist within *NIX environments!")
