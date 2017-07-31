// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import net.cakesolutions.CakeBuildInfoKeys.externalBuildTools
import sbt._
import sbt.Keys._

/**
  * Cake recommended tasks for configuring and using docker-compose within SBT
  * build files (e.g. for use within integration tests, etc.)
  */
object CakeDockerComposePlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeBuildInfoPlugin && CakeDockerPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeDockerComposePluginKeys
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
            Seq("config", "-q")
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
          Seq("up", "-d") ++
          dockerComposeUpExtras.value
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
          Seq("down", "--rmi", "all", "--volumes") ++
          dockerComposeDownExtras.value
      ).!
    if (result != 0) {
      throw new IllegalStateException(
        s"`docker-compose down` returned $result"
      )
    }
  }

  private val dockerRemoveTask: Def.Initialize[Task[Unit]] = Def.task {
    val image = (name in Docker).value
    val repository = dockerRepository.value match {
      case None =>
        image
      case Some(repo) =>
        s"$repo/$image"
    }
    val lines = "docker images".!!.split("\\n").toList
    val Line = "^([^ ]+)[ ]+([^ ]+)[ ]+([^ ]+)[ ]+.*$".r
    val ids = lines.collect {
      case Line(repo, tag, id) if repo == repository =>
        id
    }
    // only need to delete the first one (they are aliases, subsequent
    // deletes fail)
    ids.headOption.foreach { id =>
      val res = s"docker rmi -f $id".!
      if (res != 0) {
        throw new IllegalStateException(s"`docker rmi -f $id` returned $res")
      }
    }
  }

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    dockerComposeFiles := Seq(file("docker/docker-compose.yml")),
    dockerComposeImageTask := (publishLocal in Docker).value,
    dockerComposeConfigCheck := dockerComposeConfigCheckTask.value,
    dockerComposeUp := dockerComposeUpTask.value,
    dockerComposeUpExtras := Seq("--remove-orphans"),
    dockerComposeDown := dockerComposeDownTask.value,
    dockerComposeDownExtras := Seq("--remove-orphans"),
    dockerRemove := dockerRemoveTask.value,
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
object CakeDockerComposePluginKeys {

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
    taskKey[Unit]("Runs `docker-compose -f <file> up -d` for the scope")

  /**
    * Task defining how running docker-compose services will be stopped and
    * removed
    */
  val dockerComposeDown: TaskKey[Unit] =
    taskKey[Unit]("Runs `docker-compose -f <file> down` for the scope")

  /**
    * Task defining how docker images will be force removed
    */
  val dockerRemove: TaskKey[Unit] =
    taskKey[Unit](
      "Runs `docker rmi -f <ids>` for the images associated to the scope"
    )
}
