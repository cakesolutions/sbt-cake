package net.cakesolutions

import scala.concurrent.duration._
import scala.util.control.NonFatal

import sbt.IO._
import sbt.Keys._
import sbt._

object CakeDockerHealthPlugin extends AutoPlugin {
  override def requires = CakeDockerComposePlugin

  val autoImport = CakeDockerHealthPluginKeys

  import autoImport._

  private val dockerHealthTask: Def.Initialize[Task[Unit]] = Def.task {
    val url = new URL(dockerHealthEndpoint.value)

    def readLinesURLWithWait = {

      try Some(readLinesURL(url))
      catch {
        case NonFatal(_) =>
          state.value.log.info(s"Trying $url again")
          Thread.sleep(dockerHealthInterval.value.toMillis)
          None
      }
    }

    val attempts = (0 to dockerHealthRetries.value)
      .flatMap { _ =>
        readLinesURLWithWait
      }

    if (attempts.isEmpty)
      throw new IllegalStateException(s"Application at [$url] is not healthy")

  }

  override val projectSettings = Seq(
    dockerHealth := dockerHealthTask.value,
    dockerHealthInterval := 1.second,
    dockerHealthRetries := 10
  )
}

object CakeDockerHealthPluginKeys {
  val dockerHealth =
    taskKey[Unit]("Waits on the health endpoint of the application")
  val dockerHealthEndpoint = settingKey[String]("Valid Url endpoint to hit")
  val dockerHealthInterval =
    settingKey[FiniteDuration]("Amount of time to wait between health checks")
  val dockerHealthRetries =
    settingKey[Int]("Number of times to retry the health check")
}
