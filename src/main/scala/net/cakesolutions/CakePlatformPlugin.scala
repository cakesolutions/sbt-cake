// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import CakePlatformDependencies._
import sbt._
import sbt.Keys._
import wartremover._

// scalastyle:off magic.number

/**
  * Provides access to a standard set of core library dependency keys. All Cake
  * projects should enable this plugin (either directly or indirectly).
  */
object CakePlatformPlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeStandardsPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

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
      netty3
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
    libraryDependencies ++= PlatformBundles.logback,
    dependencyOverrides ++= PlatformBundles.logback.toSet,
    libraryDependencies += typesafeConfig,
    libraryDependencies ++= PlatformBundles.testing(Test),
    // the naughty list
    excludeDependencies ++= Seq(
      // we don't want another
      // https://issues.apache.org/jira/browse/CASSANDRA-10984
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

/**
  * Library dependency keys that will be auto-imported when this plugin is
  * enabled on a project.
  */
object CakePlatformKeys {

  /** Convenient bundles for depending on platform/core libraries */
  object PlatformBundles {

    // WORKAROUND https://issues.apache.org/jira/browse/CASSANDRA-10984
    //            cassandra needs a subset of netty 4
    val cassandra: Seq[ModuleID] = {
      Seq(cassandraAll exclude ("io.netty", "netty-all")) ++ Seq(
        Netty4.buffer,
        Netty4.common,
        Netty4.transport,
        Netty4.epoll classifier "linux-x86_64",
        Netty4.handler
      )
    }

    val gatling: Seq[ModuleID] = {
      Seq(Gatling.app, Gatling.highcharts, Gatling.testkit, Gatling.http)
    }

    val angularBootstrap: Seq[ModuleID] = Seq(
      Webjars.bootstrap,
      Webjars.angular,
      Webjars.leaflet,
      Webjars.angularLeaflet,
      Webjars.seiyriaSlider,
      Webjars.angularSlider
    )

    val akka: Seq[ModuleID] = Seq(Akka.actor, Akka.testkit % Test)

    val kafkaClient: Seq[ModuleID] = {
      Seq(
        ScalaKafkaClient.base,
        ScalaKafkaClient.akka,
        ScalaKafkaClient.testkit % Test
      )
    }

    val akkaCluster: Seq[ModuleID] =
      Seq(Akka.clusterSharding, Akka.clusterTools, Akka.chill)

    val akkaPersistence: Seq[ModuleID] = Seq(
      Akka.persistence,
      Akka.persistenceQuery,
      Akka.persistenceCassandra
    ) ++ cassandra.map(_ % Test)

    val akkaHttp: Seq[ModuleID] = {
      Seq(
        Akka.Http.core,
        Akka.Http.base,
        Akka.Http.testkit % Test,
        swagger,
        Jackson.databind,
        Jackson.scala
      )
    }

    val logback: Seq[ModuleID] = Seq(
      scalaLogging,
      Akka.slf4j intransitive (),
      logbackClassic,
      Slf4j.log4jOver,
      Slf4j.api,
      Slf4j.julTo,
      Slf4j.jclOver
    )

    def testing(config: Configuration): Seq[ModuleID] =
      Seq(scalatest % config, scalacheck % config) ++ logback
        .map(_ % config)
  }
}
