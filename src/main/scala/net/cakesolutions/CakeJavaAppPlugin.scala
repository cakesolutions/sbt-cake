// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt.Keys._
import sbt._

/**
  * Cake recommended settings for Java application packaging.
  */
// only for projects that use the JavaServerAppPackaging
object CakeJavaAppPlugin extends AutoPlugin {
  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.linux.Mapper._
  import com.typesafe.sbt.packager.Keys._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins =
    com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def projectSettings: Seq[Setting[_]] = Seq(
    // TODO: CO-150: Remove CakeJavaAppPlugin bash script hack
    bashScriptExtraDefines +=
      """addJava "-Duser.dir=$(realpath "$(cd "${app_home}/.."; pwd -P)")"""",
    Universal / mappings ++= {
      val jar = (Compile / packageBin).value // forces compile
      val src = sourceDirectory.value
      packageMapping((src / "main" / "resources") -> "conf")
        .withContents()
        .mappings
        .toSeq
    }
  )
}
