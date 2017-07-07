// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import play.core.PlayVersion
import sbt._
import sbt.Keys._
import wartremover._

// scalastyle:off magic.number

/**
  * Library dependency keys that will be auto-imported when this plugin is
  * enabled on a project.
  * TODO: CO-143: Ideally we should refactor all dependencies in a single place.
  */
object CakePlatformKeys {

  /**
    * Versions of platform libraries. Anything project specific can be
    * written explicitly in build.sbt and anything specific to a
    * platform concept (below) can be written explicitly there.
    *
    * We have strict conflict resolution, so it'll catch any mismatches
    * and force you to update your build.sbt
    */
  object Versions {
    val akka = "2.4.18"
    val akkaHttp = "10.0.5"
    val gatling = "2.2.3"
    val jackson = "2.8.7"
    val kafkaClient = "0.10.2.0"
    val netty4 = "4.0.45.Final"
    val play = PlayVersion.current
  }

  /** Convenient bundles for depending on platform/core libraries */
  object PlatformDependencies {
    // WORKAROUND https://issues.apache.org/jira/browse/CASSANDRA-10984
    //            cassandra needs a subset of netty 4
    val cassandra: Seq[ModuleID] = {
      Seq(
        // Update attempted to 3.10, required unclear additional properties
        // (cdc_raw_directory)
        "org.apache.cassandra" % "cassandra-all" % "3.7"
          exclude ("io.netty", "netty-all")
      ) ++ Seq(
        "io.netty" % "netty-buffer" % Versions.netty4,
        "io.netty" % "netty-common" % Versions.netty4,
        "io.netty" % "netty-transport" % Versions.netty4,
        "io.netty" % "netty-transport-native-epoll" % Versions.netty4
          classifier "linux-x86_64",
        "io.netty" % "netty-handler" % Versions.netty4
      )
    }

    val gatling: Seq[ModuleID] = {
      Seq(
        "io.gatling" % "gatling-app" % Versions.gatling,
        "io.gatling.highcharts" % "gatling-charts-highcharts"
          % Versions.gatling,
        "io.gatling" % "gatling-test-framework" % Versions.gatling,
        "io.gatling" % "gatling-http" % Versions.gatling
      )
    }

    val angularBootstrap: Seq[ModuleID] = Seq(
      "org.webjars.bower" % "bootstrap" % "3.3.7",
      "org.webjars.bower" % "angularjs" % "1.6.3",
      "org.webjars.bower" % "leaflet" % "1.0.3",
      "org.webjars.bower" % "angular-leaflet-directive" % "0.10.0",
      "org.webjars.bower" % "seiyria-bootstrap-slider" % "9.7.2",
      "org.webjars.bower" % "angular-bootstrap-slider" % "0.1.28"
    )

    val akka: Seq[ModuleID] = Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test
    )

    val kafkaClient: Seq[ModuleID] = {
      Seq(
        "net.cakesolutions" %% "scala-kafka-client" % Versions.kafkaClient,
        "net.cakesolutions" %% "scala-kafka-client-akka" % Versions.kafkaClient,
        "net.cakesolutions" %% "scala-kafka-client-testkit"
          % Versions.kafkaClient % Test
      )
    }

    val akkaCluster: Seq[ModuleID] = Seq(
      "com.typesafe.akka" %% "akka-cluster-sharding" % Versions.akka,
      "com.typesafe.akka" %% "akka-cluster-tools" % Versions.akka,
      "com.twitter" %% "chill-akka" % "0.9.2"
    )

    val akkaPersistence: Seq[ModuleID] = Seq(
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.typesafe.akka" %% "akka-persistence-query-experimental"
        % Versions.akka,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.24"
    ) ++ cassandra.map(_ % Test)

    val akkaHttp: Seq[ModuleID] = {
      Seq(
        "com.typesafe.akka" %% "akka-http-core" % Versions.akkaHttp,
        "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
        "com.typesafe.akka" %% "akka-http-testkit" % Versions.akkaHttp % Test,
        "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.9.1",
        // 1.12+ is for play 2.6.x
        "de.heikoseeberger" %% "akka-http-play-json" % "1.10.1",
        "com.fasterxml.jackson.core" % "jackson-databind" % Versions.jackson,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"
          % Versions.jackson
      )
    }

    // a Setting (depends on other Settings), so call like `shapeless.value`
    def shapeless: Def.Initialize[List[ModuleID]] = Def.setting {
      val plugins = CrossVersion
        .partialVersion(scalaVersion.value)
        .collect {
          case (2, 10) =>
            compilerPlugin(
              "org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.patch
            )
        }
        .toList

      "com.chuusai" %% "shapeless" % "2.3.2" :: plugins
    }

    val logback: Seq[ModuleID] = Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "com.typesafe.akka" %% "akka-slf4j" % Versions.akka intransitive (),
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ) ++ Seq(
      "org.slf4j" % "log4j-over-slf4j",
      "org.slf4j" % "slf4j-api",
      "org.slf4j" % "jul-to-slf4j",
      "org.slf4j" % "jcl-over-slf4j"
    ).map(_ % "1.7.25")

    def testing(config: Configuration): Seq[ModuleID] =
      Seq(
        // janino 3.0.6 is not compatible and causes
        // http://www.slf4j.org/codes.html#replay
        "org.codehaus.janino" % "janino" % "2.7.8" % config,
        "org.scalatest" %% "scalatest" % "3.0.3" % config,
        "org.scalacheck" %% "scalacheck" % "1.13.5" % config
      ) ++ logback.map(_ % config)
  }

  /**
    * Implicitly add extra methods to in scope Projects
    *
    * @param p project that Play application setting should be applied to
    */
  implicit class PlayOps(p: Project) {
    import play.sbt._
    import PlayImport.PlayKeys
    import play.twirl.sbt.Import.TwirlKeys

    /**
      * Enable Play Scala plugin, SBT style layout and a default set of
      * settings.
      *
      * @return project with Play settings and configuration applied
      */
    def enablePlay: Project =
      p.enablePlugins(PlayScala)
        // For consistency we prefer default SBT style layout
        // https://www.playframework.com/documentation/2.5.x/Anatomy
        .disablePlugins(PlayLayoutPlugin)
        .settings(
          // false positives in generated code
          scalacOptions -= "-Ywarn-unused-import",
          // lots of warts in generated code
          wartremoverExcluded in Compile ++= routes.RoutesKeys.routes
            .in(Compile)
            .value,
          PlayKeys.playMonitoredFiles ++=
            (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value
        )
  }
}

/**
  * Provides access to a standard set of core library dependency keys. All Cake
  * projects should enable this plugin (either directly or indirectly).
  */
object CakePlatformPlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeStandardsPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakePlatformKeys
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    dependencyOverrides ++= Set(
      // akka remoting only works on netty 3
      "io.netty" % "netty" % "3.10.6.Final"
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
    libraryDependencies ++= PlatformDependencies.logback,
    dependencyOverrides ++= PlatformDependencies.logback.toSet,
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    libraryDependencies ++= PlatformDependencies.testing(Test),
    // the naughty list
    excludeDependencies ++= Seq(
      // we don't want another
      // https://issues.apache.org/jira/browse/CASSANDRA-10984
      SbtExclusionRule("io.netty", "netty-all"),
      // clean up the mess made by everybody who doesn't use slf4j...
      SbtExclusionRule("org.apache.logging.log4j"),
      SbtExclusionRule("log4j"),
      SbtExclusionRule("commons-logging"),
      SbtExclusionRule("org.slf4j", "slf4j-log4j12"),
      SbtExclusionRule("org.slf4j", "slf4j-jdk14"),
      SbtExclusionRule("org.slf4j", "slf4j-jcl.jar")
    )
  )
}
