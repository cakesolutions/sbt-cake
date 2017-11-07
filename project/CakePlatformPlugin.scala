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
        "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6" % config,
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
          wartremoverExcluded in Compile ++= routes.RoutesKeys.routes
            .in(Compile)
            .value,
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
    dependencyOverrides ++= Seq(
      "io.netty" % "netty" % "3.10.6.Final" // akka remoting only works on netty 3
    ),
    // trust me, you don't ever want to get your stdlib versions out of sync...
    dependencyOverrides ++= Seq(
      // user may have a different scala provider...
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
      scalaOrganization.value % "scala-library" % scalaVersion.value,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value,
      scalaOrganization.value % "scalap" % scalaVersion.value
    ),
    // logging should be available everywhere (opt out if you really must...)
    libraryDependencies ++= deps.logback,
    dependencyOverrides ++= deps.logback,
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    libraryDependencies ++= deps.testing(Test),
    // the naughty list
    excludeDependencies ++= Seq(
      // we don't want another https://issues.apache.org/jira/browse/CASSANDRA-10984
      ExclusionRule("io.netty", "netty-all"),
      // clean up the mess made by everybody who doesn't use slf4j...
      ExclusionRule("org.apache.logging.log4j", "log4j-api-scala_2.10"),
      ExclusionRule("org.apache.logging.log4j", "log4j-liquibase"),
      ExclusionRule("org.apache.logging.log4j", "log4j-jul"),
      ExclusionRule("org.apache.logging.log4j", "log4j-iostreams"),
      ExclusionRule("org.apache.logging.log4j", "log4j-nosql"),
      ExclusionRule("org.apache.logging.log4j", "log4j-bom"),
      ExclusionRule("org.apache.logging.log4j", "log4j-osgi"),
      ExclusionRule("org.apache.logging.log4j", "log4j-api-scala_2.11"),
      ExclusionRule("org.apache.logging.log4j", "log4j-jmx-gui"),
      ExclusionRule("org.apache.logging.log4j", "log4j-taglib"),
      ExclusionRule("org.apache.logging.log4j", "log4j-web"),
      ExclusionRule("org.apache.logging.log4j", "log4j-flume-ng"),
      ExclusionRule("org.apache.logging.log4j", "log4j-jcl"),
      ExclusionRule("org.apache.logging.log4j", "log4j-to-slf4j"),
      ExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl"),
      ExclusionRule("org.apache.logging.log4j", "log4j-1.2-api"),
      ExclusionRule("org.apache.logging.log4j", "log4j-core-its"),
      ExclusionRule("org.apache.logging.log4j", "log4j-core"),
      ExclusionRule("org.apache.logging.log4j", "log4j-api"),
      ExclusionRule("org.apache.logging.log4j", "log4j"),
      ExclusionRule("log4j", "apache-log4j-extras"),
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("commons-logging", "commons-logging"),
      ExclusionRule("commons-logging", "commons-logging-api"),
      ExclusionRule("commons-logging", "commons-logging-adapters"),
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("org.slf4j", "slf4j-jdk14"),
      ExclusionRule("org.slf4j", "slf4j-jcl.jar")
    )
  )

}
