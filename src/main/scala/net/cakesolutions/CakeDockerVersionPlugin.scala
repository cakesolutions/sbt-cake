// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt._

import net.cakesolutions.internal.Version

/**
  * Plugin for checking `docker` and `docker-compose` versions on the system
  * against minimum required versions which have default values in the plugin,
  * and also configurable with provided setting keys.
  */
object CakeDockerVersionPlugin extends AutoPlugin {

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeDockerVersionKeys
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeDockerComposePlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings = Seq(
    minimumDockerVersion := (1, 13),
    minimumDockerComposeVersion := (1, 10),
    checkDockerVersion := dockerVersionTask.value,
    checkDockerComposeVersion := {
      dockerVersionTask.value
      dockerComposeVersionTask.value
    }
  )

  private def dockerVersionTask: Def.Initialize[Task[Unit]] = Def.task {

    val clientVersionOpt =
      Version.parse(
        Process(
          Seq("docker", "version", "--format", "{{.Client.Version}}")
        ).!!.trim
      )
    val minDockerVersion = Version.fromTuple(minimumDockerVersion.value)

    require(
      clientVersionOpt.exists(_.checkDocker(minDockerVersion)),
      s"current docker client version $clientVersionOpt, " +
        s"required minimum version $minDockerVersion"
    )

    val serverVersionOpt =
      Version.parse(
        Process(
          Seq("docker", "version", "--format", "{{.Server.Version}}")
        ).!!.trim
      )
    val clientServerVersionMatch = for {
      cv <- clientVersionOpt
      sv <- serverVersionOpt
    } yield cv.gte(sv)

    require(
      clientServerVersionMatch.exists(identity),
      s"client version ($clientVersionOpt) and " +
        s"server version ($serverVersionOpt) do not match"
    )
  }

  private def dockerComposeVersionTask: Def.Initialize[Task[Unit]] = Def.task {

    val composeVersionOpt =
      Version.parse(
        Process(Seq("docker-compose", "version", "--short")).!!.trim
      )
    val minDockerComposeVersion =
      Version.fromTuple(minimumDockerComposeVersion.value)

    require(
      composeVersionOpt.exists(_.checkDockerCompose(minDockerComposeVersion)),
      s"current docker-compose version $composeVersionOpt, " +
        s"required minimum version $minDockerComposeVersion"
    )
  }

}

/**
  * Keys that will be auto-imported when this plugin is enabled.
  */
object CakeDockerVersionKeys {

  /**
    * Task that ensures docker version on the environment
    * is higher than minimum required docker version.
    */
  val checkDockerVersion: TaskKey[Unit] =
    taskKey[Unit]("Checks docker client and server versions")

  /**
    * Task that ensures docker-compose version on the environment
    * is higher than minimum required docker-compose version.
    */
  val checkDockerComposeVersion: TaskKey[Unit] =
    taskKey[Unit]("Checks docker-compose version")

  /**
    * Minimum required docker version.
    * Represented as (major, minor) version numbers.
    */
  val minimumDockerVersion: SettingKey[(Int, Int)] =
    settingKey[(Int, Int)](
      "Minimum `docker` version as a tuple like (major, minor)"
    )

  /**
    * Minimum required docker-compose version.
    * Represented as (major, minor) version numbers.
    */
  val minimumDockerComposeVersion: SettingKey[(Int, Int)] =
    settingKey[(Int, Int)](
      "Minimum `docker-compose` version as a tuple like (major, minor)"
    )

}
