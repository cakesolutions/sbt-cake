// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import java.util.concurrent.atomic.AtomicLong

import scala.util.Properties

import sbt._
import sbt.IO._
import sbt.Keys._

// only for projects that use the DockerPlugin
object CakeDockerPlugin extends AutoPlugin {
  import com.typesafe.sbt.packager.Keys._
  import com.typesafe.sbt.packager.docker._
  import com.typesafe.sbt.SbtNativePackager._

  override def requires = DockerPlugin
  override def trigger = allRequirements
  override def projectSettings = Seq(
    dockerBaseImage := "cakesolutions/openjdk-agents:1.2",
    dockerUpdateLatest := true,
    dockerRepository := None,
    packageName in Docker := name.value,
    maintainer in Docker := "Cake Solutions <devops@cakesolutions.net>",
    version in Docker := sys.props.get("tag").getOrElse(version.value)
  )
}
