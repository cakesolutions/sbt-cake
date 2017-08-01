// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions
// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0

import java.util.concurrent.atomic.AtomicLong

import scala.util._

import sbt._
import sbt.IO._
import sbt.Keys._

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import scoverage.ScoverageKeys._

/**
 * Common plugin that sets up common variables and build settings such
 * as: handling build-related environment variables, java runtime
 * flags, parallelisation of tests, registering our repositories.
 */
object CakeBuildPlugin extends AutoPlugin {
  override def requires = sbtdynver.DynVerPlugin
  override def trigger = allRequirements

  val autoImport = CakeBuildKeys
  import autoImport._

  private val JavaSpecificFlags =
    sys.props("java.version").substring(0, 3) match {
      case "1.6" | "1.7" => List("-XX:MaxPermSize=256m")
      case _             => List("-XX:MaxMetaspaceSize=256m")
    }

  override val buildSettings = Seq(
    organization := "net.cakesolutions",
    scalaVersion := "2.11.11",
    maxErrors := 1,
    fork := true,
    cancelable := true,
    sourcesInBase := false,
    javaOptions += s"-Dcake.sbt.root=${(baseDirectory in ThisBuild).value.getCanonicalFile}",
    // WORKAROUND DockerPlugin doesn't like '+'
    version := version.value.replace('+', '-'),
    concurrentRestrictions := {
      val limited = Properties.envOrElse("SBT_TASK_LIMIT", "4").toInt
      Seq(Tags.limitAll(limited))
    }
  )

  override val projectSettings = Seq(
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    ivyLoggingLevel := UpdateLogging.Quiet,
    conflictManager := ConflictManager.strict,
    // makes it really easy to use a RAM disk
    target := {
      sys.env.get("SBT_VOLATILE_TARGET") match {
        case None => target.value
        case Some(base) =>
          file(base) / target.value.getCanonicalPath.replace(':', '_')
      }
    },
    javaOptions ++= {
      sys.env.get("SBT_VOLATILE_TARGET") match {
        case None => Nil
        case Some(base) =>
          val tmpdir = s"$base/java.io.tmpdir"
          file(tmpdir).mkdirs()
          s"-Djava.io.tmpdir=$tmpdir" :: Nil
      }
    },
    javaOptions += s"-Dcake.sbt.name=${name.value}",
    // prefer a per-application logback.xml in resources
    // javaOptions in Compile += s"-Dlogback.configurationFile=${(baseDirectory in ThisBuild).value}/logback-main.xml",
    javaOptions ++= JavaSpecificFlags ++ Seq("-Xss2m", "-Dfile.encoding=UTF8"),
    dependencyOverrides ++= Set(
      // scala-lang is always used during transitive ivy resolution (and potentially thrown out...)
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      // user may have a different scala provider...
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
      scalaOrganization.value % "scala-library" % scalaVersion.value,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value,
      scalaOrganization.value % "scalap" % scalaVersion.value
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true,
    coverageExcludedFiles := ".*/target/.*",
    coverageExcludedPackages := "controllers.javascript*;controllers.ref*;router*"
  ) ++ inConfig(Test)(sensibleTestSettings) ++ inConfig(Compile)(
      sensibleCrossPath
    )

}

object CakeBuildKeys {
  implicit class IntegrationTestOps(p: Project) {
    def enableIntegrationTests: Project = p
      .configs(IntegrationTest)
      .settings(inConfig(IntegrationTest)(Defaults.testSettings ++ sensibleTestSettings ++ scalafmtSettings))
  }

  // WORKAROUND https://github.com/sbt/sbt/issues/2534
  // don't forget to also call testLibs
  def sensibleTestSettings = sensibleCrossPath ++ Seq(
    parallelExecution := true,
    javaOptions ~= (_.filterNot(_.startsWith("-Dlogback.configurationFile"))),
    javaOptions += s"-Dlogback.configurationFile=${(baseDirectory in ThisBuild).value}/logback-${configuration.value}.xml",
    // play overrides the slf4j / logback config with their own magic
    // https://www.playframework.com/documentation/2.6.x/SettingsLogger#Using--Dlogger.file
    javaOptions += s"-Dlogger.file=${(baseDirectory in ThisBuild).value}/logback-${configuration.value}.xml",
    testForkedParallel := true,
    testGrouping := {
      val opts = ForkOptions(
        bootJars = Nil,
        javaHome = javaHome.value,
        connectInput = connectInput.value,
        outputStrategy = outputStrategy.value,
        runJVMOptions = javaOptions.value,
        workingDirectory = Some(baseDirectory.value),
        envVars = envVars.value
      )
      definedTests.value.map { test =>
        Tests.Group(test.name, Seq(test), Tests.SubProcess(opts))
      }
    },
    javaOptions ++= {
      if (sys.env.get("GC_LOGGING").isEmpty) Nil
      else {
        val base = (baseDirectory in ThisBuild).value
        val config = configuration.value
        val n = name.value
        val count = forkCount.incrementAndGet() // subject to task evaluation
        val out = { base / s"gc-$config-$n.log" }.getCanonicalPath
        Seq(
          // https://github.com/fommil/lions-share
          s"-Xloggc:$out",
          "-XX:+PrintGCDetails",
          "-XX:+PrintGCDateStamps",
          "-XX:+PrintTenuringDistribution",
          "-XX:+PrintHeapAtGC"
        )
      }
    },
    // and don't forget `export SCALACTIC_FILE_PATHNAMES=true`
    testOptions += Tests
      .Argument(TestFrameworks.ScalaTest, "-oFD", "-W", "120", "60"),
    testFrameworks := Seq(TestFrameworks.ScalaTest, TestFrameworks.JUnit)
  )

  // used for unique gclog naming
  private[this] val forkCount = new AtomicLong()

  // WORKAROUND https://github.com/sbt/sbt/issues/2819
  private[cakesolutions] def sensibleCrossPath = Seq(
    unmanagedSourceDirectories += {
      val dir = scalaSource.value
      val Some((major, minor)) =
        CrossVersion.partialVersion(scalaVersion.value)
      file(s"${dir.getPath}-$major.$minor")
    }
  )

}
