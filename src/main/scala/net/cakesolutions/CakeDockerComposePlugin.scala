package net.cakesolutions

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

  private val dockerComposeUpTask: Def.Initialize[Task[Unit]] = Def.task {
    val _ = dockerComposeImageTask.value
    val input = dockerComposeFile.value.getCanonicalPath
    val res = s"docker-compose -f $input up -d".!
    if (res != 0)
      throw new IllegalStateException(
        s"docker-compose up returned $res (are you sure all image deps are in build.sbt?)"
      )
  }

  private val dockerComposeDownTask: Def.Initialize[Task[Unit]] = Def.task {
    val input = dockerComposeFile.value.getCanonicalPath
    val res = s"docker-compose -f $input down".!
    if (res != 0)
      throw new IllegalStateException(s"docker-compose down returned $res")
  }

  private val dockerRemoveTask: Def.Initialize[Task[Unit]] = Def.task {
    val image = (name in Docker).value
    val repository = dockerRepository.value match {
      case None => image
      case Some(repo) => s"${repo}/${image}"
    }
    val lines = "docker images".!!.split("\\n").toList
    val Line = "^([^ ]+)[ ]+([^ ]+)[ ]+([^ ]+)[ ]+.*$".r
    val ids = lines.collect {
      case Line(repo, tag, id) if repo == repository => id
    }
    // only need to delete the first one (they are aliases, subsequent
    // deletes fail)
    ids.headOption.foreach { id =>
      val res = s"docker rmi -f $id".!
      if (res != 0)
        throw new IllegalStateException(s"docker rmi -f $id returned $res")
    }
  }

  override val projectSettings = Seq(
    // we trigger dockerRepository on envvar because we do not want it
    // in publishLocal, but it gets picked up there, so we only pass
    // the envvar when doing the final publish.
    dockerRepository := sys.env
      .get("DOCKER_REPOSITORY")
      .orElse(Some((name in ThisBuild).value)),
    dockerComposeFile := file(s"docker/${name.value}.yml"),
    dockerComposeImageTask := (publishLocal in Docker).value,
    dockerComposeUp := dockerComposeUpTask.value,
    dockerComposeDown := dockerComposeDownTask.value,
    dockerRemove := dockerRemoveTask.value
  )

}

object CakeDockerComposePluginKeys {
  val dockerComposeFile =
    settingKey[File]("docker-compose.yml file to use in dockerComposeUp.")
  val dockerComposeImageTask =
    taskKey[Unit]("Publishes the images used by dockerComposeUp.")
  val dockerComposeUp =
    taskKey[Unit]("Runs `docker-compose -f <file> up -d` for the scope")
  val dockerComposeDown =
    taskKey[Unit]("Runs `docker-compose -f <file> down` for the scope")
  val dockerRemove =
    taskKey[Unit](
      "Runs `docker rmi -f <ids>` for the images associated to the scope"
    )
}
