// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions
// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0

import sbt._
import sbt.Keys._
import play.core.PlayVersion

object CakePlatformKeys {

  /**
   * Versions of platform libraries. Anything project specific can be
   * written explicitly in build.sbt and anything specific to a
   * platform concept (below) can be written explicitly there.
   *
   * We have strict conflict resolution, so it'll catch any mismatches
   * and force you to update your build.sbt
   */
  object versions {
    val akka = "2.4.17"
    val play = PlayVersion.current
  }

  /** Convenient bundles for depending on platform / core libraries */
  object deps {
    // WORKAROUND https://issues.apache.org/jira/browse/CASSANDRA-10984
    //            cassandra needs a subset of netty 4
    val Cassandra = {
      val netty4 = "4.0.45.Final"
      Seq(
        // Update attempted to 3.10, required unclear additional properties (cdc_raw_directory)
        "org.apache.cassandra" % "cassandra-all" % "3.7" exclude ("io.netty", "netty-all")
      ) ++ Seq(
          "io.netty" % "netty-buffer" % netty4,
          "io.netty" % "netty-common" % netty4,
          "io.netty" % "netty-transport" % netty4,
          "io.netty" % "netty-transport-native-epoll" % netty4 classifier ("linux-x86_64"),
          "io.netty" % "netty-handler" % netty4
        )
    }

    val Gatling = {
      val version = "2.2.3"
      Seq(
        "io.gatling" % "gatling-app" % version,
        "io.gatling.highcharts" % "gatling-charts-highcharts" % version,
        "io.gatling" % "gatling-test-framework" % version,
        "io.gatling" % "gatling-http" % version
      )
    }

    val AngularBootstrap = Seq(
      "org.webjars.bower" % "bootstrap" % "3.3.7",
      "org.webjars.bower" % "angularjs" % "1.6.3",
      "org.webjars.bower" % "leaflet" % "1.0.3",
      "org.webjars.bower" % "angular-leaflet-directive" % "0.10.0",
      "org.webjars.bower" % "seiyria-bootstrap-slider" % "9.7.2",
      "org.webjars.bower" % "angular-bootstrap-slider" % "0.1.28"
    )

    val Akka = Seq(
      "com.typesafe.akka" %% "akka-actor" % versions.akka,
      "com.typesafe.akka" %% "akka-testkit" % versions.akka % Test
    )

    val KafkaClient = {
      val v = "0.10.2.0"
      Seq(
        "net.cakesolutions" %% "scala-kafka-client" % v,
        "net.cakesolutions" %% "scala-kafka-client-akka" % v,
        "net.cakesolutions" %% "scala-kafka-client-testkit" % v % Test
      )
    }

    val AkkaCluster = Seq(
      "com.typesafe.akka" %% "akka-cluster-sharding" % versions.akka,
      "com.typesafe.akka" %% "akka-cluster-tools" % versions.akka,
      "com.twitter" %% "chill-akka" % "0.9.2"
    )

    val AkkaPersistence = Seq(
      "com.typesafe.akka" %% "akka-persistence" % versions.akka,
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % versions.akka,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.24"
    ) ++ Cassandra.map(_ % Test)

    val AkkaHttp = {
      val akka_http = "10.0.5"
      val jackson = "2.8.7"
      Seq(
        "com.typesafe.akka" %% "akka-http-core" % akka_http,
        "com.typesafe.akka" %% "akka-http" % akka_http,
        "com.typesafe.akka" %% "akka-http-testkit" % akka_http % Test,
        "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.9.1",
        "de.heikoseeberger" %% "akka-http-play-json" % "1.10.1", // 1.12+ is for play 2.6.x
        "com.fasterxml.jackson.core" % "jackson-databind" % jackson,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jackson
      )
    }

    // a Setting (depends on other Settings), so call like `shapeless.value`
    def shapeless = Def.setting {
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

    val logback = Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "com.typesafe.akka" %% "akka-slf4j" % versions.akka intransitive (),
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
        "org.scalatest" %% "scalatest" % "3.0.2" % config,
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
      SbtExclusionRule("org.apache.logging.log4j"),
      SbtExclusionRule("log4j"),
      SbtExclusionRule("commons-logging"),
      SbtExclusionRule("org.slf4j", "slf4j-log4j12"),
      SbtExclusionRule("org.slf4j", "slf4j-jdk14"),
      SbtExclusionRule("org.slf4j", "slf4j-jcl.jar")
    )
  )

}
