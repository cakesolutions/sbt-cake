// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

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
    CakeTestRunnerPlugin)
  .settings(
    dependencyOverrides ++= Set(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.7",
      "com.typesafe.akka" % "akka-actor_2.11" % "2.4.19"
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      Akka.Http.base,
      Akka.Http.core,
      Akka.Http.sprayJson,
      Akka.Http.testkit % Test,
      GatlingModule.app,
      GatlingModule.highcharts,
      GatlingModule.testkit,
      GatlingModule.http,
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

healthCheckIntervalInSeconds := 1
healthCheckRetryCount := 10

val nullLogger = new ProcessLogger {
  def info(s: => String): Unit = ()
  def error(s: => String): Unit = ()
  def buffer[T](f: => T): T = f
}

// We use SBT build tasks to ensure that the mock server actually runs in a
// separate process fork!

val startServer = taskKey[Unit]("Start mock issue management server")
startServer := {
  Process("./start-server.sh").run(nullLogger)
}

val stopServer = taskKey[Unit]("Stop mock issue management server")
stopServer := {
  Process("./stop-server.sh").run(nullLogger)
}
