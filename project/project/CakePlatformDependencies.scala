// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import sbt._

/**
  * Dependencies referenced in sbt-cake plugins
  */
object CakePlatformDependencies {

  // Update attempted to 3.10, required unclear additional properties
  // (cdc_raw_directory)
  val cassandraAll: ModuleID = "org.apache.cassandra" % "cassandra-all" % "3.11.1"
  val guava: ModuleID = "com.google.guava" % "guava" % "23.0"
  val httpClient: ModuleID =
    "org.apache.httpcomponents" % "httpclient" % "4.5.5"
  val levelDbJni: ModuleID = "org.fusesource.leveldbjni" % "leveldbjni" % "1.8"
  val logbackClassic: ModuleID = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val netty3: ModuleID = "io.netty" % "netty" % "3.10.6.Final"
  val quasiQuotes: ModuleID = "org.scalamacros" %% "quasiquotes" % "2.1.1"
  val scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % "1.13.5"
  val scalaLogging: ModuleID =
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.4"
  val shapeless: ModuleID = "com.chuusai" %% "shapeless" % "2.3.3"
  val swagger: ModuleID =
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.12.0"
  val typesafeConfig: ModuleID = "com.typesafe" % "config" % "1.3.2"

  object Akka {
    val version = "2.5.9"

    val actor: ModuleID = "com.typesafe.akka" %% "akka-actor" % version
    val chill: ModuleID = "com.twitter" %% "chill-akka" % "0.9.2"
    val clusterSharding: ModuleID =
      "com.typesafe.akka" %% "akka-cluster-sharding" % version
    val clusterTools: ModuleID =
      "com.typesafe.akka" %% "akka-cluster-tools" % version
    val persistence: ModuleID =
      "com.typesafe.akka" %% "akka-persistence" % version
    val persistenceCassandra: ModuleID =
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.80"
    val persistenceQuery: ModuleID =
        "com.typesafe.akka" %% "akka-persistence-query" % version
    val slf4j: ModuleID = "com.typesafe.akka" %% "akka-slf4j" % version
    val stream: ModuleID = "com.typesafe.akka" %% "akka-stream" % version
    val testkit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % version

    object Http {
      val version = "10.0.11"

      val base: ModuleID = "com.typesafe.akka" %% "akka-http" % version
      val core: ModuleID = "com.typesafe.akka" %% "akka-http-core" % version
      val sprayJson: ModuleID =
        "com.typesafe.akka" %% "akka-http-spray-json" % version
      val testkit: ModuleID =
        "com.typesafe.akka" %% "akka-http-testkit" % version
    }
  }

  object ApacheCommons {
    val codec: ModuleID = "commons-codec" % "commons-codec" % "1.11"
    val compress: ModuleID = "org.apache.commons" % "commons-compress" % "1.15"
    val lang3: ModuleID = "org.apache.commons" % "commons-lang3" % "3.7"
    val logging: ModuleID = "commons-logging" % "commons-logging" % "1.2"
  }

  object Gatling {
    val version = "2.3.0"

    val app: ModuleID = "io.gatling" % "gatling-app" % version
    val highcharts: ModuleID =
      "io.gatling.highcharts" % "gatling-charts-highcharts" % version
    val http: ModuleID = "io.gatling" % "gatling-http" % version
    val testkit: ModuleID = "io.gatling" % "gatling-test-framework" % version
  }

  object Jackson {
    val version = "2.9.4"

    val databind: ModuleID =
      "com.fasterxml.jackson.core" % "jackson-databind" % version
    val scala: ModuleID =
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % version
  }

  object Netty4 {
    val version = "4.1.21.Final"

    val buffer: ModuleID = "io.netty" % "netty-buffer" % version
    val common: ModuleID = "io.netty" % "netty-common" % version
    val epoll: ModuleID = "io.netty" % "netty-transport-native-epoll" % version
    val handler: ModuleID = "io.netty" % "netty-handler" % version
    val transport: ModuleID = "io.netty" % "netty-transport" % version
  }

  object SbtDependencies {
    val buildInfo: ModuleID = "com.eed3si9n" % "sbt-buildinfo" % "0.9.0"
    val digest: ModuleID = "com.typesafe.sbt" % "sbt-digest" % "1.1.4"
    val dynver: ModuleID = "com.dwijnand" % "sbt-dynver" % "2.0.0"
    val gatling: ModuleID = "io.gatling" % "gatling-sbt" % "2.2.2"
    val git: ModuleID = "com.typesafe.sbt" % "sbt-git" % "0.9.3"
    val gzip: ModuleID = "com.typesafe.sbt" % "sbt-gzip" % "1.0.2"
    val header: ModuleID = "de.heikoseeberger" % "sbt-header" % "4.1.0"
    val packager: ModuleID =
      "com.typesafe.sbt" % "sbt-native-packager" % "1.3.2"
    val pgp: ModuleID = "com.jsuereth" % "sbt-pgp" % "1.1.0"
    val scalafix: ModuleID = "ch.epfl.scala" % "sbt-scalafix" % "0.5.10"
    val scalafmt: ModuleID = "com.lucidchart" % "sbt-scalafmt-coursier" % "1.15"
    val scalastyle: ModuleID =
      "org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0"
    val scoverage: ModuleID = "org.scoverage" % "sbt-scoverage" % "1.5.1"
    val sonatype: ModuleID = "org.xerial.sbt" % "sbt-sonatype" % "2.0"

    object Coursier {
      val version = "1.0.0"

      val sbt: ModuleID = "io.get-coursier" % "sbt-coursier" % version
      val cache: ModuleID = "io.get-coursier" %% "coursier-cache" % version
      val core: ModuleID = "io.get-coursier" %% "coursier" % version
    }
  }

  object ScalaKafkaClient {
    val version = "1.0.0"

    val akka: ModuleID =
      "net.cakesolutions" %% "scala-kafka-client-akka" % version
    val base: ModuleID = "net.cakesolutions" %% "scala-kafka-client" % version
    val testkit: ModuleID =
      "net.cakesolutions" %% "scala-kafka-client-testkit" % version
  }

  object Slf4j {
    val version = "1.7.25"

    val api: ModuleID = "org.slf4j" % "slf4j-api" % version
    val jclOver: ModuleID = "org.slf4j" % "jcl-over-slf4j" % version
    val julTo: ModuleID = "org.slf4j" % "jul-to-slf4j" % version
    val log4jOver: ModuleID = "org.slf4j" % "log4j-over-slf4j" % version
  }

  object Webjars {
    val angular: ModuleID = "org.webjars.bower" % "angularjs" % "1.6.8"
    val angularLeaflet: ModuleID =
      "org.webjars.bower" % "angular-leaflet-directive" % "0.10.0"
    val angularSlider: ModuleID =
      "org.webjars.bower" % "angular-bootstrap-slider" % "0.1.28"
    val bootstrap: ModuleID = "org.webjars.bower" % "bootstrap" % "3.3.7"
    val leaflet: ModuleID = "org.webjars.bower" % "leaflet" % "1.0.3"
    val locator: ModuleID = "org.webjars" % "webjars-locator" % "0.26"
    val seiyriaSlider: ModuleID =
      "org.webjars.bower" % "seiyria-bootstrap-slider" % "9.7.2"
  }
}
