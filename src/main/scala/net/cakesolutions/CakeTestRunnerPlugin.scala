// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import io.gatling.sbt.GatlingKeys.GatlingIt
import sbt.Keys._
import sbt._

import net.cakesolutions.CakeDockerHealthKeys._
import net.cakesolutions.CakeDockerPluginKeys.dockerRemove
import net.cakesolutions.CakeDockerVersionKeys.checkDockerComposeVersion

/**
  * Plugin for running integration and performance tests with docker setup.
  * When tests are triggered, these operations are done respectively;
  * - docker and docker-compose version checks
  * - docker-compose up
  * - health check of containers in docker-compose scope
  * - running tests
  * - dumping logs of containers
  * - docker-compose down
  * - docker remove containers in docker-compose scope
  *
  * After docker-compose up step, if there is an error, after taking
  * containers dump, containers are stopped and removed.
  */
object CakeTestRunnerPlugin extends AutoPlugin {

  import net.cakesolutions.internal.CakeDockerUtils._

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeTestRunnerKeys
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeDockerComposePlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    healthCheckIntervalInSeconds := 5,
    healthCheckRetryCount := 5,
    integrationTests := integrationTestsTask.value,
    performanceTests := performanceTestsTask.value
  )

  private def runIntegrationTests: Def.Initialize[Task[Unit]] = Def.taskDyn {
    (test in IntegrationTest)
  }

  /**
    * Performance Tests are defined based on IntegrationTests
    * and GatlingIt (gatling integration test) settings.
    */
  private def runPerformanceTests: Def.Initialize[Task[Unit]] = Def.taskDyn {
    (test in GatlingIt)
  }

  // TODO: Need to check first, if `enableIntegrationTests` is set on project
  private def integrationTestsTask: Def.Initialize[Task[Unit]] =
    runTestTaskWithDocker(runIntegrationTests)

  // TODO: Need to check first, if `enablePerformanceTests` is set on project
  private def performanceTestsTask: Def.Initialize[Task[Unit]] =
    runTestTaskWithDocker(runPerformanceTests)

  import CakeDockerComposeKeys._

  /**
    * Complete test task life cycle.
    * All required steps (tasks) are sequentially defined.
    * @param testTask can be integration or performance
    */
  private def runTestTaskWithDocker(
    testTask: Def.Initialize[Task[Unit]]
  ): Def.Initialize[Task[Unit]] = {
    Def.sequential(
      checkDockerComposeVersion,
      dockerComposeUp,
      healthCheck,
      Def.taskDyn {
        testTask.doFinally {
          cleanStop.taskValue
        }
      }
    )
  }

  /**
    * Sequential tasks for cleaning the test environment
    * after a success or an error case.
    *
    * - dumping logs
    * - docker compose down (remove all containers in scope)
    * - docker remove (remove all images in scope)
    */
  private def cleanStop: Def.Initialize[Task[Unit]] =
    Def.sequential(dumpContainersLogs, dockerComposeDown, dockerRemove)

  /**
    * Checking the health status of all containers in scope
    * at each pre-defined interval as pre-defined retry count.
    */
  private def healthCheck: Def.Initialize[Task[Unit]] = Def.taskDyn {

    import scala.concurrent.duration._

    def check(rc: Int): Boolean = {
      val healthy =
        checkHealth(CakeDockerComposeKeys.dockerComposeFiles.value)(
          streams.value.log,
          CakeBuildInfoKeys.projectRoot.value,
          dockerComposeEnvVars.value
        )
      if (!healthy && rc > 0) {
        Thread.sleep(healthCheckIntervalInSeconds.value.seconds.toMillis)
        check(rc - 1)
      } else {
        healthy
      }
    }

    val isHealthy = check(healthCheckRetryCount.value)

    if (isHealthy) {
      Def.task(nop)
    } else {
      cleanStop.andFinally {
        throw new IllegalStateException(
          "Containers in scope are not healthy to continue!"
        )
      }
    }
  }

}

/**
  * Keys that will be auto-imported when this plugin is enabled.
  */
object CakeTestRunnerKeys {

  /**
    * Task that runs integration tests in complete docker lifecycle
    */
  val integrationTests: TaskKey[Unit] =
    taskKey[Unit]("Runs integration tests with docker fleet management")

  /**
    * Task that runs performance tests in complete docker lifecycle
    */
  val performanceTests: TaskKey[Unit] =
    taskKey[Unit]("Runs performance tests with docker fleet management")

  /**
    * Retries count for health check in docker-compose scope
    */
  val healthCheckRetryCount: SettingKey[Int] =
    settingKey[Int](
      "Retries count for health check in docker-compose scope"
    )

  /**
    * Interval time in seconds for health check in docker-compose scope
    */
  val healthCheckIntervalInSeconds: SettingKey[Int] =
    settingKey[Int](
      "Interval time in seconds for health check in docker-compose scope"
    )

}
