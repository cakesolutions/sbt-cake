// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt.Keys._
import sbt._
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
      wartRemoverSettings ++
      customJavacOptions ++
      Seq(
        scalacOptions ++= commonScalacOptions ++ {
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, 12)) =>
              scalacOptionsFor212
            case Some((2, 11)) =>
              scalacOptionsFor211
            case _ =>
              Nil
          }
        } ++
          // fatal warnings can get in the way during the DEV cycle
          sys.env
            .get("CI")
            .fold(Seq.empty[String])(_ => Seq("-Xfatal-warnings"))
      )

  private lazy val customJavacOptions = Seq(
    javacOptions ++=
      Seq(
        "-Xlint:all",
        "-Xlint:-options",
        "-Xlint:-path",
        "-Xlint:-processing"
      ) ++
        sys.env.get("CI").fold(Seq.empty[String])(_ => Seq("-Werror")),
    // some of those flags are not supported in doc
    javacOptions in doc ~= (_.filterNot(_.startsWith("-Xlint")))
  )

  private lazy val wartRemoverSettings = {
    // http://www.wartremover.org
    val warts = Warts.unsafe ++
      Seq(Wart.FinalCaseClass, Wart.ExplicitImplicitTypes)
    Seq(
      wartremoverErrors in (Compile, compile) := warts,
      wartremoverWarnings in (Test, compile) := warts,
      wartremoverWarnings in (IntegrationTest, compile) := warts,
      wartremoverExcluded in Compile ++= (managedSources in Compile).value
    )
  }

  // See https://tpolecat.github.io/2017/04/25/scalac-flags.html
  private lazy val scalacOptionsFor212 = Seq(
    "-Xlint:constant",
    "-Ywarn-extra-implicit",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates"
  )

  private lazy val scalacOptionsFor211 = Seq(
    "-Yinline-warnings",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  )

  private lazy val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",
    "-Xcheckinit",
    "-Xfuture",
    "-Xlint:adapted-args",
    "-Xlint:by-name-right-associative",
    "-Xlint:delayedinit-select",
    "-Xlint:doc-detached",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow",
    "-Xlint:unsound-match",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )
}
