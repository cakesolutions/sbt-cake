// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt._
import sbt.Keys._
import wartremover._

// scalastyle:off magic.number

/**
  * Adds coding standards to every build, e.g. linting, formatting.
  */
object CakeStandardsPlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeBuildPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  // TODO: CO-71: Remove manual loading of ScalafmtCorePlugin
  override val buildSettings: Seq[Setting[_]] =
    ScalafmtCorePlugin.buildSettings

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] =
    ScalafmtCorePlugin.projectSettings ++
      Seq(Compile, Test).flatMap(inConfig(_)(scalafmtSettings)) ++
      Seq(
        scalacOptions ++=
          Seq(
            "-encoding",
            "UTF-8",
            "-feature",
            "-deprecation",
            "-unchecked",
            "-language:postfixOps",
            "-language:implicitConversions",
            "-Xlint",
            "-Yno-adapted-args",
            "-Ywarn-dead-code",
            "-Xfuture"
          ) ++ {
            CrossVersion.partialVersion(scalaVersion.value) match {
              case Some((2, 12)) =>
                Seq("-Ywarn-unused-import", "-Ypartial-unification")
              case Some((2, 11)) =>
                Seq(
                  "-Yinline-warnings",
                  "-Ywarn-unused-import",
                  "-Ypartial-unification"
                )
              case Some((2, 10)) =>
                Seq("-Yinline-warnings")
              case _ =>
                Nil
            }
          } ++
            // fatal warnings can get in the way during the DEV cycle
            sys.env
              .get("CI")
              .fold(Seq.empty[String])(_ => Seq("-Xfatal-warnings")),
        javacOptions ++=
          Seq(
            "-Xlint:all",
            "-Xlint:-options",
            "-Xlint:-path",
            "-Xlint:-processing"
          ) ++
            sys.env.get("CI").fold(Seq.empty[String])(_ => Seq("-Werror")),
        // some of those flags are not supported in doc
        javacOptions in doc ~= (_.filterNot(_.startsWith("-Xlint"))),
        // http://www.wartremover.org
        wartremoverExcluded in Compile ++= (managedSources in Compile).value,
        wartremoverWarnings in (Compile, compile) :=
          Warts.unsafe ++ Seq(Wart.FinalCaseClass, Wart.ExplicitImplicitTypes)
      )
}
