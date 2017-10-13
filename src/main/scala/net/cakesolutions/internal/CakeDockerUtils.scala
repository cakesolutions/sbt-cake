// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

package internal

import scala.util.{Failure, Success, Try}

import sbt._

/**
  * Util trait which gathers checking docker and docker-compose versions,
  * health checking for containers and dumping logs operations.
  */
private[cakesolutions] object CakeDockerUtils {

  /**
    * Check the health status of all containers defined in docker-compose scope
    *
    * @param ymlFiles docker-compose files which defines scope
    * @param log sbt logger
    * @return either all containers are healthy or not
    */
  def checkHealth(ymlFiles: Seq[File])(implicit log: Logger): Boolean = {
    containerIds(ymlFiles).forall(isHealthy)
  }

  private def isHealthy(containerId: String)(implicit log: Logger): Boolean = {
    val health =
      Seq("docker", "inspect", "--format", "{{.State.Health}}", containerId)
    val status = Seq(
      "docker",
      "inspect",
      "--format",
      "{{.State.Health.Status}}",
      containerId
    )

    Try(health.!!.trim) match {
      case Success(h) if h.equalsIgnoreCase("<nil>") =>
        log.warn(
          s"Ignoring health checks for container $containerId: " +
            "as no health checking is defined"
        )
        true
      case Success(_) =>
        Try(status.!!.trim) match {
          case Success(s) if s.equalsIgnoreCase("healthy") =>
            log.info(s"Container $containerId is healthy")
            true
          case _ =>
            log.error(s"Container $containerId is NOT healthy")
            false
        }
      case Failure(exn) =>
        log.error(
          s"Failed to get health info for container $containerId - reason: $exn"
        )
        false
    }
  }

  /**
    * Dumping logs for all containers defined in docker-compose scope
    *
    * @param ymlFiles docker-compose files which defines scope
    * @param targetDir target directory for log files
    * @param log sbt logger
    */
  def dumpLogs(ymlFiles: Seq[File], targetDir: File)(
    implicit log: Logger
  ): Unit = {
    containerIds(ymlFiles).foreach { containerId =>
      val dockerTargetDir = file(targetDir.getAbsolutePath + "/docker")

      val cName = containerName(containerId)

      val logFileName = s"$cName-$containerId.log"

      log.info(
        s"Dumping logs of containerName: $cName, id: $containerId " +
          s"to ${dockerTargetDir.getAbsolutePath}."
      )

      IO.createDirectory(dockerTargetDir)
      val logFile = file(dockerTargetDir.getAbsolutePath + s"/$logFileName")

      Try(Seq("docker", "logs", containerId).!(new FileLogger(logFile))) match {
        case Success(_) =>
          log.success(s"Dumped logs of $containerId successfully.")
        case Failure(exn) =>
          log.error(s"Failed to fetch logs for $containerId: $exn")
      }
    }
  }

  private class FileLogger(logFile: File) extends ProcessLogger {
    IO.write(logFile, "")

    def buffer[T](f: => T): T = f

    def error(s: => String): Unit =
      IO.append(logFile, s"$s\n".getBytes("utf-8"))

    def info(s: => String): Unit =
      IO.append(logFile, s"$s\n".getBytes("utf-8"))
  }

  private def containerName(id: String): String =
    Seq("docker", "ps", "-a", "--format", "{{.Names}}", "-f", s"id=$id").!!.trim

  private def containerIds(
    ymlFiles: Seq[File]
  )(implicit log: Logger): Seq[String] = {

    val projectOverrides =
      ymlFiles.flatMap(yaml => Seq("-f", yaml.getCanonicalPath))

    val projectName =
      sys.env
        .get("DOCKER_COMPOSE_PROJECT_NAME")
        .fold(Seq.empty[String])(name => Seq("-p", name))

    val listContainers =
      Seq("docker-compose") ++
        projectName ++
        projectOverrides ++
        Seq("ps", "-q")

    Try(listContainers.!!.trim.split("\\s").toSeq) match {
      case Failure(exn) =>
        log.error(s"Failed to fetch container identities: $exn")
        throw exn
      case Success(ids) =>
        ids.filter(_.nonEmpty)
    }
  }
}

/**
  * A helper ADT to represent docker version information.
  */
private[cakesolutions] final case class Version(major: Int, minor: Int) {

  /**
    * TODO:
    *
    * @param that
    * @return
    */
  def gte(that: Version): Boolean = {
    major > that.major || (major == that.major && minor >= that.minor)
  }

  /**
    * TODO:
    *
    * @param minVersion
    * @return
    */
  def checkDockerCompose(minVersion: Version): Boolean = gte(minVersion)

  /**
    * TODO:
    *
    * @param minVersion
    * @return
    */
  def checkDocker(minVersion: Version): Boolean = gte(minVersion)
}

private[cakesolutions] object Version {

  private val versionRegex = """([0-9]+)\.([0-9]+)[ .,-]""".r

  /**
    * TODO:
    *
    * @param text
    * @return
    */
  def parse(text: String): Option[Version] = {
    versionRegex.findFirstIn(text).map {
      case versionRegex(majorStr, minorStr) =>
        Version(majorStr.toInt, minorStr.toInt)
    }
  }

  /**
    * TODO:
    *
    * @param v
    * @return
    */
  def fromTuple(v: (Int, Int)): Version = Version(v._1, v._2)

  /**
    * Method that determines what the latest version is.
    *
    * @param first first version to compare against
    * @param second second version to compare against
    * @return latest version
    */
  def selectLatest(first: (Int, Int), second: (Int, Int)): (Int, Int) = {
    if (fromTuple(first).gte(fromTuple(second))) {
      first
    } else {
      second
    }
  }
}
