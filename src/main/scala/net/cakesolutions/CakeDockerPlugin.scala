// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import sbt.Keys._
import sbt._

// only for projects that use the DockerPlugin
object CakeDockerPlugin extends AutoPlugin {
  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.Keys._
  import com.typesafe.sbt.packager.docker._

  override def requires = CakeBuildInfoPlugin && DockerPlugin
  override def trigger = allRequirements
  override def projectSettings = Seq(
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    dockerRepository := None,
    packageName in Docker := name.value,
    maintainer in Docker := "Cake Solutions <devops@cakesolutions.net>",
    version in Docker := sys.props.get("tag").getOrElse(version.value),
    dockerCommands += {
      val dockerArgList =
        CakeBuildInfoKeys.generalInfo.value ++ CakeBuildInfoKeys.dockerInfo.value
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
