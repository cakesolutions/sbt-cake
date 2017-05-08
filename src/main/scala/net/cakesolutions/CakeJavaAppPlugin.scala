// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import java.util.concurrent.atomic.AtomicLong

import scala.util.Properties

import sbt._
import sbt.IO._
import sbt.Keys._

// only for projects that use the JavaServerAppPackaging
object CakeJavaAppPlugin extends AutoPlugin {
  import com.typesafe.sbt.packager.Keys._
  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.linux.Mapper._

  override def requires =
    com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
  override def trigger = allRequirements
  override def projectSettings = Seq(
    mappings in Universal ++= {
      val jar = (packageBin in Compile).value // forces compile
      val src = sourceDirectory.value
      packageMapping(
        (src / "main" / "resources") -> "conf"
      ).withContents().mappings.toSeq
    }
  )

}
