// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import java.util.concurrent.atomic.AtomicLong

import scala.util._

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import io.gatling.sbt.GatlingPlugin
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._

/**
  * Common plugin that sets up common variables and build settings such as:
  * - handling build-related environment variables
  * - java runtime flags
  * - parallelisation of tests
  * - registering our repositories.
  */
object CakeBuildPlugin extends AutoPlugin {
  import CakeDynVerPlugin.{autoImport => DynVer}

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeDynVerPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeBuildKeys
  import autoImport._

  private val JavaSpecificFlags =
    sys.props("java.version").substring(0, 3) match {
      case "1.6" | "1.7" => List("-XX:MaxPermSize=256m")
      case _ => List("-XX:MaxMetaspaceSize=256m")
    }

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val buildSettings: Seq[Setting[_]] = Seq(
    organization := "net.cakesolutions",
    scalaVersion := "2.12.4",
    maxErrors := 1,
    fork := true,
    cancelable := true,
    sourcesInBase := false,
    javaOptions +=
      s"-Dcake.sbt.root=${(baseDirectory in ThisBuild).value.getCanonicalFile}",
    // WORKAROUND DockerPlugin doesn't like '+', so we need to ensure both
    // version and dynver are transformed in the same way
    DynVer.dynver := DynVer.dynver.value.replace('+', '-'),
    version := version.value.replace('+', '-'),
    concurrentRestrictions := {
      val limited =
        Try(sys.env.getOrElse("SBT_TASK_LIMIT", "4").toInt).getOrElse {
          throw new IllegalArgumentException(
            "SBT_TASK_LIMIT should be an integer value"
          )
        }
      Seq(Tags.limitAll(limited))
    }
  )

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    ivyLoggingLevel := UpdateLogging.Quiet,
    conflictManager := ConflictManager.strict,
    // makes it really easy to use a RAM disk - when the environment variable
    // exists, the SBT_VOLATILE_TARGET/target directory is created as a side
    // effect
    target := {
      sys.env.get("SBT_VOLATILE_TARGET") match {
        case None => target.value
        case Some(base) =>
          file(base) / target.value.getCanonicalPath.replace(':', '_')
      }
    },
    // When the environment variable exists, the
    // SBT_VOLATILE_TARGET/java.io.tmpdir directory is created as a side effect
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
    // javaOptions in Compile +=
    //   "-Dlogback.configurationFile=" +
    //     s"${(baseDirectory in ThisBuild).value}/logback-main.xml",
    javaOptions ++= JavaSpecificFlags ++ Seq("-Xss2m", "-Dfile.encoding=UTF8"),
    dependencyOverrides ++= Set(
      // scala-lang is always used during transitive ivy resolution (and
      // potentially thrown out...)
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
    coverageExcludedPackages :=
      "controllers.javascript*;controllers.ref*;router*"
  ) ++
    inConfig(Test)(sensibleTestSettings) ++
    inConfig(Compile)(sensibleCrossPath)
}

/**
  * Build keys that will be auto-imported when this plugin is enabled.
  */
object CakeBuildKeys {

  /**
    * Implicitly add extra methods to in scope Projects
    *
    * @param p project on which to apply integration test settings
    */
  implicit class IntegrationTestOps(p: Project) {

    /**
      * Enable integration test configuration and a default set of settings.
      *
      * @return Project with integration test settings and configuration applied
      */
    def enableIntegrationTests: Project =
      p.configs(IntegrationTest)
        .settings(
          inConfig(IntegrationTest)(
            Defaults.testSettings ++ sensibleTestSettings ++ scalafmtSettings
          )
        )

    /**
      * Enable performance test on top of integration test settings
      * and GatlingPlugin.
      *
      * @return Project with performance test settings and configuration applied
      */
    def enablePerformanceTests: Project =
      p.enableIntegrationTests
        .enablePlugins(GatlingPlugin)
  }

  /**
    * SBT project settings for configuring testing.
    *
    * @return Cake recommended test settings
    */
  // WORKAROUND https://github.com/sbt/sbt/issues/2534
  // don't forget to also call testLibs
  def sensibleTestSettings: Seq[Def.Setting[_]] =
    sensibleCrossPath ++
      Seq(
        parallelExecution in Test := true,
        parallelExecution in IntegrationTest := false,
        javaOptions ~= (_.filterNot(
          _.startsWith("-Dlogback.configurationFile")
        )),
        javaOptions += {
          val baseDir = (baseDirectory in ThisBuild).value
          val config = configuration.value
          s"-Dlogback.configurationFile=$baseDir/logback-$config.xml"
        },
        testForkedParallel in Test := true,
        testForkedParallel in IntegrationTest := false,
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
          if (sys.env.get("GC_LOGGING").isEmpty) {
            Nil
          } else {
            val base = (baseDirectory in ThisBuild).value
            val config = configuration.value
            val n = name.value
            // subject to task evaluation
            val count = forkCount.incrementAndGet()
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
