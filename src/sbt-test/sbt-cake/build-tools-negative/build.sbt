// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

name in ThisBuild := "build-tools-negative"

enablePlugins(CakeBuildInfoPlugin)

externalBuildTools :=
  Seq("impossible-tool" -> "Fake error - `impossible-tool` should not exist!")
