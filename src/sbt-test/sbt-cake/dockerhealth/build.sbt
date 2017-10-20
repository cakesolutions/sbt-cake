// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakeBuildInfoKeys._
import net.cakesolutions.CakePlatformDependencies._

name in ThisBuild := "dockerhealth"

val dockerhealth = (project in file("."))
  .enablePlugins(
    AshScriptPlugin,
    CakeDockerPlugin,
    CakeDockerHealthPlugin)
  .settings(
    libraryDependencies ++= Seq(
      Akka.Http.base,
      Akka.Http.core,
      Akka.Http.sprayJson,
      Akka.Http.testkit % Test,
      Jackson.databind,
      Jackson.scala,
      swagger
    )
  )
  .settings(
    mainClass in Compile := Some("MockServer")
  )

externalBuildTools :=
  Seq(
    "docker --version" -> "`docker` should exist",
    "docker-compose --version" -> "`docker-compose` should exist"
  )
