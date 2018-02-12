// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt.Keys._
import sbt._

import net.cakesolutions.CakeDockerComposeKeys._
import net.cakesolutions.CakeDockerVersionKeys.{minimumDockerComposeVersion, minimumDockerVersion}
import net.cakesolutions.internal.Version

/**
  * Plugin for checking `docker` and `docker-compose` versions on the system
  * against minimum required versions which have default values in the plugin,
  * and also configurable with provided setting keys.
  */
object CakeDockerHealthPlugin extends AutoPlugin {

  import net.cakesolutions.internal.CakeDockerUtils._

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeDockerHealthKeys
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins =
    CakeDockerComposePlugin && CakeDockerVersionPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings = Seq(
    // Docker 1.12.0 (2016-07-28) introduced HEALTHCHECK
    // see https://docs.docker.com/release-notes/docker-engine/#1120-2016-07-28
    minimumDockerVersion :=
      Version.selectLatest(minimumDockerVersion.value, (1, 12)),
    // Docker-compose 1.10.0 (2017-01-18) introduced healthcheck
    // see https://docs.docker.com/release-notes/docker-compose/#1100-2017-01-18
    minimumDockerComposeVersion :=
      Version.selectLatest(minimumDockerComposeVersion.value, (1, 10)),
    dumpContainersLogs := dumpLogs(
      dockerComposeFiles.value,
      file("target")
    )(
      streams.value.log,
      CakeBuildInfoKeys.projectRoot.value,
      dockerComposeEnvVars.value
    ),
    checkContainersHealth := {
      require(
        checkHealth(dockerComposeFiles.value)(
          streams.value.log,
          CakeBuildInfoKeys.projectRoot.value,
          dockerComposeEnvVars.value
        ),
        "All containers should be healthy"
      )
    }
  )

}

/**
  * Keys that will be auto-imported when this plugin is enabled.
  */
object CakeDockerHealthKeys {

  /**
    * Task that dumps the logs of each container in the
    * docker-compose scope.
    */
  val dumpContainersLogs: TaskKey[Unit] =
    taskKey[Unit]("Dumps target containers' logs")

  /**
    * Task that checks the health status of each container in the
    * docker-compose scope and they should be in healthy state.
    */
  val checkContainersHealth: TaskKey[Unit] =
    taskKey[Unit]("Checks target containers' health")

}
