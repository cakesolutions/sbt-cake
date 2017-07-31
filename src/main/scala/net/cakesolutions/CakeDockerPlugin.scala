// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt._
import sbt.Keys._

/**
  * Cake recommended settings for configuring linter and wartremover, along
  * with a standard suite of compiler compatibility flags.
  */
object CakeDockerPlugin extends AutoPlugin {
  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.Keys._
  import com.typesafe.sbt.packager.docker._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeBuildInfoPlugin && DockerPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def projectSettings: Seq[Setting[_]] = Seq(
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    // we trigger dockerRepository on envvar because we do not want it
    // in publishLocal, but it gets picked up there, so we only pass
    // the envvar when doing the final publish.
    dockerRepository :=
      sys.env
        .get("DOCKER_REPOSITORY")
        .orElse(Some((name in ThisBuild).value)),
    packageName in Docker := name.value,
    maintainer in Docker := "Cake Solutions <devops@cakesolutions.net>",
    version in Docker := sys.props.get("tag").getOrElse(version.value),
    dockerCommands += {
      val dockerArgList =
        CakeBuildInfoKeys.generalInfo.value ++
          CakeBuildInfoKeys.dockerInfo.value
      val labelArguments =
        dockerArgList
          .map {
            case (key, value) =>
              s""""${name.value}.$key"="$value""""
          }

      Cmd("LABEL", labelArguments.mkString(" "))
    }
  )
}
