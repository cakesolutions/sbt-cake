// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import com.typesafe.sbt.SbtNativePackager._
import sbt.Keys._
import sbt._

import net.cakesolutions.CakeBuildInfoKeys.{externalBuildTools, projectRoot}

/**
  * Cake recommended tasks for configuring and using docker-compose within SBT
  * build files (e.g. for use within integration tests, etc.)
  */
object CakeDockerComposePlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeBuildInfoPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeDockerComposeKeys
  import autoImport._

  private val dockerComposeConfigCheckTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val projectOverrides =
        dockerComposeFiles.value.flatMap(
          yaml => Seq("-f", yaml.getCanonicalPath)
        )
      val projectName =
        sys.env
          .get("DOCKER_COMPOSE_PROJECT_NAME")
          .fold(Seq.empty[String])(name => Seq("-p", name))
      val result =
        Process(
          Seq("docker-compose") ++
            projectName ++
            projectOverrides ++
            Seq("config", "-q"),
          projectRoot.value,
          dockerComposeEnvVars.value.toSeq: _*
        ).!
      if (result != 0) {
        throw new IllegalStateException(
          "Failed to validate docker-compose YAML configuration"
        )
      }
    }

  private val dockerComposeUpTask: Def.Initialize[Task[Unit]] = Def.task {
    val _ = dockerComposeImageTask.value
    val projectOverrides =
      dockerComposeFiles.value.flatMap(
        yaml => Seq("-f", yaml.getCanonicalPath)
      )
    val projectName =
      sys.env
        .get("DOCKER_COMPOSE_PROJECT_NAME")
        .fold(Seq.empty[String])(name => Seq("-p", name))
    val result =
      Process(
        Seq("docker-compose") ++
          projectName ++
          projectOverrides ++
          Seq("up", dockerComposeUpLaunchStyle.value) ++
          dockerComposeUpExtras.value,
        projectRoot.value,
        dockerComposeEnvVars.value.toSeq: _*
      ).!
    if (result != 0) {
      throw new IllegalStateException(
        s"`docker-compose up` returned $result (are you sure all image " +
          "dependencies are in build.sbt?)"
      )
    }
  }

  private val dockerComposeDownTask: Def.Initialize[Task[Unit]] = Def.task {
    val projectOverrides =
      dockerComposeFiles.value.flatMap(
        yaml => Seq("-f", yaml.getCanonicalPath)
      )
    val projectName =
      sys.env
        .get("DOCKER_COMPOSE_PROJECT_NAME")
        .fold(Seq.empty[String])(name => Seq("-p", name))
    val result =
      Process(
        Seq("docker-compose") ++
          projectName ++
          projectOverrides ++
          Seq("down") ++
          dockerComposeDownExtras.value,
        projectRoot.value,
        dockerComposeEnvVars.value.toSeq: _*
      ).!
    if (result != 0) {
      throw new IllegalStateException(
        s"`docker-compose down` returned $result"
      )
    }
  }

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    dockerComposeFiles := Seq(file("docker/docker-compose.yml")),
    dockerComposeEnvVars := Map(),
    dockerComposeImageTask := (publishLocal in Docker).value,
    dockerComposeConfigCheck := dockerComposeConfigCheckTask.value,
    dockerComposeUp := dockerComposeUpTask.value,
    dockerComposeUpLaunchStyle := "-d",
    dockerComposeUpExtras := Seq("--remove-orphans"),
    dockerComposeDown := dockerComposeDownTask.value,
    dockerComposeDownExtras := {
      if (sys.env.get("CI").isDefined) {
        Seq("--rmi", "all", "--volumes")
      } else {
        Seq("--volumes")
      }
    },
    externalBuildTools ++=
      Seq(
        (
          "docker version",
          "`docker` needs to be installed, PATH accessible and able to " +
            "contact a Docker registry"
        ),
        (
          "docker-compose version",
          "`docker-compose` needs to be installed and PATH accessible"
        )
      )
  )
}

/**
  * SBT docker-compose build settings and tasks
  */
object CakeDockerComposeKeys {

  /**
    * Setting key defining the project files to be used by docker-compose
    * commands. Files here will be composed using docker-compose project
    * overrides.
    */
  val dockerComposeFiles: SettingKey[Seq[File]] =
    settingKey[Seq[File]](
      "docker-compose YAML files to use in dockerComposeUp"
    )

  /**
    * Setting key defining the environment variables to be set by SBT when
    * launching the docker-compose project.
    */
  val dockerComposeEnvVars: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]](
      "docker-compose build environment variables"
    )

  /**
    * Setting key defining the type or style of docker-compose launching. This
    * setting can be used to define if containers are launched in daemon mode
    * or aborting if any container exits (with or without the exit code of a
    * specific service).
    */
  val dockerComposeUpLaunchStyle: SettingKey[String] =
    settingKey[String](
      "Launch style for the dockerComposeUp task"
    )

  /**
    * Setting key defining extra arguments that will be added to the
    * dockerComposeUp command
    */
  val dockerComposeUpExtras: SettingKey[Seq[String]] =
    settingKey[Seq[String]](
      "Additional arguments for the dockerComposeUp task"
    )

  /**
    * Setting key defining extra arguments that will be added to the
    * dockerComposeDown command
    */
  val dockerComposeDownExtras: SettingKey[Seq[String]] =
    settingKey[Seq[String]](
      "Additional arguments for the dockerComposeDown task"
    )

  /**
    * Task defining how we will validate the docker-compose YAML files
    */
  val dockerComposeConfigCheck: TaskKey[Unit] =
    taskKey[Unit]("Validate the docker-compose YAML file")

  /**
    * Task defining how docker images will be locally published
    */
  val dockerComposeImageTask: TaskKey[Unit] =
    taskKey[Unit]("Publishes the images used by dockerComposeUp")

  /**
    * Task defining how docker-compose services will be launched
    */
  val dockerComposeUp: TaskKey[Unit] =
    taskKey[Unit]("Runs `docker-compose -f <file> up` for the scope")

  /**
    * Task defining how running docker-compose services will be stopped and
    * removed
    */
  val dockerComposeDown: TaskKey[Unit] =
    taskKey[Unit]("Runs `docker-compose -f <file> down` for the scope")
}
