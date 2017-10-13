// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakeBuildInfoKeys._
import net.cakesolutions.CakePlatformDependencies

name in ThisBuild := "dockerhealth"

val dockerhealth = (project in file("."))
  .enablePlugins(
    AshScriptPlugin,
    CakeDockerPlugin,
    CakeDockerHealthPlugin)
  .settings(
    libraryDependencies ++=
      PlatformBundles.akkaHttp :+
        CakePlatformDependencies.Akka.Http.sprayJson
  )
  .settings(
    mainClass in Compile := Some("MockServer")
  )

externalBuildTools :=
  Seq(
    "docker --version" -> "`docker` should exist",
    "docker-compose --version" -> "`docker-compose` should exist"
  )
