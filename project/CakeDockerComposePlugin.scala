package net.cakesolutions

import scala.util.Try
import scala.sys.process._

import sbt._
import sbt.Keys._

import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.SbtNativePackager._

object CakeDockerComposePlugin extends AutoPlugin {
  override def requires = CakeDockerPlugin

  override def trigger = allRequirements

  val autoImport = CakeDockerComposePluginKeys
  import autoImport._

  def projectName =
    sys.env
      .get("DOCKER_COMPOSE_PROJECT_NAME")
      .fold("")(name => s"-p $name")

  val dockerComposeTestQuickTask: Def.Initialize[Task[Unit]] = Def.sequential(
    dockerComposeUpQuick,
    test,
    dockerComposeDown
  )

  val dockerComposeUpTask: Def.Initialize[Task[Unit]] = Def.sequential(
    dockerComposeImages,
    dockerComposeUpQuick
  )

  val dockerComposeUpQuickTask: Def.Initialize[Task[Unit]] = Def.task {
    val input = dockerComposeFile.value.getCanonicalPath
    val res = s"docker-compose $projectName -f $input up -d".!
    if (res != 0)
      throw new IllegalStateException(s"docker-compose up returned $res")
  }

  val dockerComposeDownTask: Def.Initialize[Task[Unit]] = Def.task {
    val input = dockerComposeFile.value.getCanonicalPath
    s"docker-compose $projectName -f $input kill -s 9".! // much faster
    val res = s"docker-compose $projectName -f $input down".!
    if (res != 0)
      throw new IllegalStateException(s"docker-compose down returned $res")
  }

  // this task is very flakey and should never throw an exception, to
  // avoid breaking the build workflow.
  val dockerRemoveTask: Def.Initialize[Task[Unit]] = Def.task {
    val image = (name in Docker).value
    val repository = dockerRepository.value match {
      case None       => image
      case Some(repo) => s"${repo}/${image}"
    }
    var limit = 100
    def doit(): List[String] = {
      if (limit == 0) throw new IllegalStateException("deleted too many times")
      limit -= 1
      val lines = "docker images".!!.split("\\n").toList
      val Line = "^([^ ]+)[ ]+([^ ]+)[ ]+([^ ]+)[ ]+.*$".r
      val ids = lines.collect {
        case Line(repo, tag, id) if repo == repository => id
      }
      // alias deletes often fail, so only delete the head and loop
      ids.headOption.foreach { id =>
        s"docker rmi -f $id".!
      }
      ids
    }
    Try {
      while (doit().nonEmpty) {}
    }
  }

  override val projectSettings = Seq(
    // we trigger dockerRepository on envvar because we do not want it
    // in publishLocal, but it gets picked up there, so we only pass
    // the envvar when doing the final publish.
    dockerRepository := sys.env
      .get("DOCKER_REPOSITORY")
      .orElse(Some((name in ThisBuild).value)),
    dockerRemove := dockerRemoveTask.value
  )

  // manually added per test inConfig
  def dockerComposeSettings = Seq(
    dockerComposeImages := {},
    dockerComposeUpQuick := dockerComposeUpQuickTask.value,
    dockerComposeUp := dockerComposeUpTask.value,
    dockerComposeDown := dockerComposeDownTask.value,
    dockerComposeFile := file(
      s"docker/${name.value}-${configuration.value}.yml"
    )
  )
}

object CakeDockerComposePluginKeys {
  val dockerComposeTestQuick =
    taskKey[Unit]("brings up docker, runs 'test', brings down docker")

  val dockerComposeFile =
    settingKey[File]("docker-compose.yml file to use in dockerComposeUp.")
  val dockerComposeImages =
    taskKey[Unit]("Publishes the images used by dockerComposeUp.")
  val dockerComposeUpQuick =
    taskKey[Unit]("Runs `docker-compose -f <file> up -d` for the scope")
  val dockerComposeUp =
    taskKey[Unit](
      "Builds the images and then runs `docker-compose -f <file> up -d` for the scope")
  val dockerComposeDown =
    taskKey[Unit]("Runs `docker-compose -f <file> down` for the scope")
  val dockerRemove =
    taskKey[Unit](
      "Runs `docker rmi -f <ids>` for the images associated to the scope"
    )
}
