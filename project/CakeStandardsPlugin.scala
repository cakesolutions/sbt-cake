// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions
// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0

import java.util.concurrent.atomic.AtomicLong

import scala.util.Properties

import sbt._
import sbt.IO._
import sbt.Keys._

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import wartremover._

/**
  * Adds coding standards to every build, e.g. linting, formatting.
  */
object CakeStandardsPlugin extends AutoPlugin {
  override def requires = CakeBuildPlugin
  override def trigger = allRequirements

  val autoImport = CakeStandardsKeys
  import autoImport._

  override val buildSettings = Seq(
    scalafmtVersion := "1.3.0"
  )

  override val projectSettings = Seq(
    scalacOptions ++= Seq(
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
          Seq("-Yinline-warnings",
              "-Ywarn-unused-import",
              "-Ypartial-unification")
        case Some((2, 10)) => Seq("-Yinline-warnings")
        case _             => Nil
      }
    } ++ {
      // fatal warnings can get in the way during the DEV cycle
      if (sys.env.contains("CI")) Seq("-Xfatal-warnings")
      else Nil
    },
    javacOptions ++= Seq(
      "-Xlint:all",
      "-Xlint:-options",
      "-Xlint:-path",
      "-Xlint:-processing"
    ) ++ {
      if (sys.env.contains("CI")) Seq("-Werror")
      else Nil
    },
    // some of those flags are not supported in doc
    javacOptions in doc ~= (_.filterNot(_.startsWith("-Xlint"))),
    // http://www.wartremover.org
    wartremoverExcluded in Compile ++= (managedSources in Compile).value,
    wartremoverWarnings in (Compile, compile) :=
      Warts.unsafe ++ Seq(
        Wart.FinalCaseClass,
        Wart.ExplicitImplicitTypes
      )
  )

}

object CakeStandardsKeys
