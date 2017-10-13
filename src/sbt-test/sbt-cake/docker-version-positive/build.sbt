// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakeBuildInfoKeys

name in ThisBuild := "docker-version-positive"

enablePlugins(CakeDockerVersionPlugin)

CakeBuildInfoKeys.externalBuildTools :=
  Seq(
    "docker --version" -> "`docker` should exist",
    "docker-compose --version" -> "`docker-compose` should exist"
  )
