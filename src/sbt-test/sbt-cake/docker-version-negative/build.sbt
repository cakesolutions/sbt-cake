// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakeBuildInfoKeys

name in ThisBuild := "docker-version-negative"

enablePlugins(CakeDockerVersionPlugin)

CakeBuildInfoKeys.externalBuildTools :=
  Seq(
    "docker --version" -> "`docker` should exist",
    "docker-compose --version" -> "`docker-compose` should exist"
  )

minimumDockerVersion := (30, 40)

minimumDockerComposeVersion := (30, 40)
