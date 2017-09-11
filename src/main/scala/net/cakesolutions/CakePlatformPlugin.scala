// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions
// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0

import sbt._
import sbt.Keys._
import play.core.PlayVersion
import wartremover._

object CakePlatformKeys {

  /** Convenient bundles for depending on platform / core libraries */
  object deps {
    val logback = Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ) ++ Seq(
        "org.slf4j" % "log4j-over-slf4j",
        "org.slf4j" % "slf4j-api",
        "org.slf4j" % "jul-to-slf4j",
        "org.slf4j" % "jcl-over-slf4j"
      ).map(_ % "1.7.25")

    def testing(config: Configuration) =
      Seq(
        // janino 3.0.6 is not compatible and causes http://www.slf4j.org/codes.html#replay
        "org.codehaus.janino" % "janino" % "2.7.8" % config,
        "org.scalatest" %% "scalatest" % "3.0.4" % config,
        "org.scalacheck" %% "scalacheck" % "1.13.5" % config
      ) ++ logback.map(_ % config)
  }

  implicit class PlayOps(p: Project) {
    import play.sbt._
    import PlayImport.PlayKeys
    import play.twirl.sbt.Import.TwirlKeys

    // for consistency we prefer default SBT style layout
    // https://www.playframework.com/documentation/2.5.x/Anatomy
    def enablePlay: Project =
      p.enablePlugins(PlayScala)
        .disablePlugins(PlayLayoutPlugin)
        .settings(
          // false positives in generated code
          scalacOptions -= "-Ywarn-unused-import",
          // lots of warts in generated code
          wartremoverExcluded in Compile ++= routes.RoutesKeys.routes.in(Compile).value,
          PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value
        )
  }

}

object CakePlatformPlugin extends AutoPlugin {
  override def requires = CakeStandardsPlugin
  override def trigger = allRequirements

  val autoImport = CakePlatformKeys
  import autoImport._

  override val buildSettings = Seq()

  override val projectSettings = Seq(
    dependencyOverrides ++= Set(
      "io.netty" % "netty" % "3.10.6.Final" // akka remoting only works on netty 3
    ),
    // trust me, you don't ever want to get your stdlib versions out of sync...
    dependencyOverrides ++= Set(
      // user may have a different scala provider...
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
      scalaOrganization.value % "scala-library" % scalaVersion.value,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value,
      scalaOrganization.value % "scalap" % scalaVersion.value
    ),
    // logging should be available everywhere (opt out if you really must...)
    libraryDependencies ++= deps.logback,
    dependencyOverrides ++= deps.logback.toSet,
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    libraryDependencies ++= deps.testing(Test),
    // the naughty list
    excludeDependencies ++= Seq(
      // we don't want another https://issues.apache.org/jira/browse/CASSANDRA-10984
      SbtExclusionRule("io.netty", "netty-all"),
      // clean up the mess made by everybody who doesn't use slf4j...
      SbtExclusionRule("org.apache.logging.log4j", "log4j-api-scala_2.10"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-liquibase"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-jul"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-iostreams"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-nosql"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-bom"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-osgi"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-api-scala_2.11"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-jmx-gui"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-taglib"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-web"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-flume-ng"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-jcl"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-to-slf4j"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-1.2-api"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-core-its"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-core"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j-api"),
      SbtExclusionRule("org.apache.logging.log4j", "log4j"),
      SbtExclusionRule("log4j", "apache-log4j-extras"),
      SbtExclusionRule("log4j", "log4j"),
      SbtExclusionRule("commons-logging", "commons-logging"),
      SbtExclusionRule("commons-logging", "commons-logging-api"),
      SbtExclusionRule("commons-logging", "commons-logging-adapters"),
      SbtExclusionRule("org.slf4j", "slf4j-log4j12"),
      SbtExclusionRule("org.slf4j", "slf4j-jdk14"),
      SbtExclusionRule("org.slf4j", "slf4j-jcl.jar")
    )
  )

}
