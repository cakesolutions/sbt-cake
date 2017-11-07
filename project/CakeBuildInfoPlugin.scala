// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import scala.util._

import sbt._
import sbt.IO._
import sbt.Keys._

import sbtbuildinfo.BuildInfoPlugin, BuildInfoPlugin.autoImport._

// kicks in if the Project uses the BuildInfoPlugin
object CakeBuildInfoPlugin extends AutoPlugin {
  override def requires = BuildInfoPlugin
  override def trigger = allRequirements

  override val projectSettings = Seq(
    buildInfoPackage := s"${organization.value}.${name.value}.build",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion
    ),
    buildInfoOptions := Seq(BuildInfoOption.ToJson),
    buildInfoOptions ++= {
      if (sys.env.contains("SBT_IGNORE_BUILDTIME")) Nil
      else Seq(BuildInfoOption.BuildTime)
    }
  )

}
