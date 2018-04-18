// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import scala.sys.process._

import net.cakesolutions.CakePlatformDependencies.{Gatling => GatlingModule, _}
import net.cakesolutions.CakeBuildInfoKeys._
import net.cakesolutions.CakeTestRunnerKeys._

name in ThisBuild := "testrunner"

val testrunner = (project in file("."))
  .enableIntegrationTests
  .enablePerformanceTests
  .enablePlugins(
    BuildInfoPlugin,
    DockerPlugin,
    AshScriptPlugin,
    CakeBuildPlugin,
    CakeDockerPlugin,
    CakeTestRunnerPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      Akka.actor,
      Akka.stream,
      Akka.Http.base,
      Akka.Http.sprayJson,
      Akka.Http.testkit % Test,
      GatlingModule.app,
      GatlingModule.highcharts,
      GatlingModule.testkit,
      GatlingModule.http,
      Jackson.databind,
      Jackson.scala,
      swagger,
      scalatest % Test
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

healthCheckIntervalInSeconds := 1
healthCheckRetryCount := 10
