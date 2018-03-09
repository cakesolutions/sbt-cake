// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt._
import scoverage._, ScoverageKeys._

object CakeCoveragePlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = ScoverageSbtPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    coverageMinimum := 80,
    coverageFailOnMinimum := true,
    coverageExcludedFiles := ".*/target/.*",
    coverageExcludedPackages :=
      "controllers.javascript*;controllers.ref*;router*"
  )
}
