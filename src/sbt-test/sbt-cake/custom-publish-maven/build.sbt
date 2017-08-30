// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

import scala.util.matching.Regex

import complete.DefaultParsers._
import net.cakesolutions.CakePublishMavenPluginKeys._
import net.cakesolutions.DynVerPattern

name in ThisBuild := "custom-publish-maven"

enablePlugins(CakeBuildPlugin, CakePublishMavenPlugin)

snapshotRepositoryResolver :=
  Some(Resolver.file("file", new File(s"${target.value}/snapshot")))

repositoryResolver :=
  Some(Resolver.file("file", new File(s"${target.value}/release")))

val serviceName = "XXX"

dynVerPattern in ThisBuild := DynVerPattern(
  s"${serviceName}_",
  s"""(${serviceName}_[0-9][^+]*)""".r,
  s"${serviceName}_[0-9]*"
)

mkVersion in ThisBuild := {
  case None =>
    s"${serviceName}_${timestamp(dynverCurrentDate.value)}-SNAPSHOT"

  case Some(out) =>
    (
      out.ref.value.startsWith(dynVerPattern.value.tagPrefix),
      out.commitSuffix.distance
    ) match {
      case (true, 0) =>
        out.ref.value
      case (true, _) =>
        s"${out.ref.value}_${out.commitSuffix.sha}-SNAPSHOT"
      case (false, _) =>
        s"${serviceName}_${out.ref.value}-SNAPSHOT"
    }
}

def timestamp(d: Instant): String =
  DateTimeFormatter
    .ofPattern("yyyyMMddHHmm")
    .withZone(ZoneId.of("UTC"))
    .format(d)

val tagBranch: InputKey[Unit] =
  inputKey[Unit]("Task that tags branch with an integer")

tagBranch in ThisBuild := {
  val buildNum: Int = spaceDelimited("<arg>").parsed.head.toInt

  (s"git tag -a XXX_${buildNum}_${timestamp(dynverCurrentDate.value)} " +
    s"-m $buildNum").!!
}
