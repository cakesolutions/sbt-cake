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
  override def trigger: PluginTrigger = allRequirements

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeDockerComposePluginKeys
  import autoImport._

  private val dockerComposeUpTask: Def.Initialize[Task[Unit]] = Def.task {
    val _ = dockerComposeImageTask.value
    val input = dockerComposeFile.value.getCanonicalPath
    val res = s"docker-compose -f $input up -d".!
    if (res != 0) {
      throw new IllegalStateException(
        s"`docker-compose up` returned $res (are you sure all image " +
          "dependencies are in build.sbt?)"
      )
    }
  }

  private val dockerComposeDownTask: Def.Initialize[Task[Unit]] = Def.task {
    val input = dockerComposeFile.value.getCanonicalPath
    val res = s"docker-compose -f $input down".!
    if (res != 0) {
      throw new IllegalStateException(s"`docker-compose down` returned $res")
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
    // we trigger dockerRepository on envvar because we do not want it
    // in publishLocal, but it gets picked up there, so we only pass
    // the envvar when doing the final publish.
    dockerRepository :=
      sys.env
        .get("DOCKER_REPOSITORY")
        .orElse(Some((name in ThisBuild).value)),
    dockerComposeFile := file(s"docker/${name.value}.yml"),
    dockerComposeImageTask := (publishLocal in Docker).value,
    dockerComposeUp := dockerComposeUpTask.value,
    dockerComposeDown := dockerComposeDownTask.value,
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
    * Setting key defining the file to be used by docker-compose commands
    */
  val dockerComposeFile: SettingKey[File] =
    settingKey[File]("docker-compose.yml file to use in dockerComposeUp")

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
