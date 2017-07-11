// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import sbt._

/**
  * Dependencies referenced in sbt-cake build.sbt
  * TODO: CO-143: Ideally we should refactor all dependencies in a single place.
  */
object CakeDependencies {

  // TODO: CO-68: remove JSR305 dependency when SBT moves away from Scala 2.10
  val jsr305 = "com.google.code.findbugs" % "jsr305" % "3.0.2"

  object SbtDependencies {
    val buildInfo = "com.eed3si9n" % "sbt-buildinfo" % "0.7.0"
    val coursier = "io.get-coursier" % "sbt-coursier" % "1.0.0-RC6"
    val digest = "com.typesafe.sbt" % "sbt-digest" % "1.1.1"
    val dynver = "com.dwijnand" % "sbt-dynver" % "1.3.0"
    val git = "com.typesafe.sbt" % "sbt-git" % "0.9.2"
    val gzip = "com.typesafe.sbt" % "sbt-gzip" % "1.0.0"
    val jsEngine = "com.typesafe.sbt" % "sbt-js-engine" % "1.1.4"
    val packager = "com.typesafe.sbt" % "sbt-native-packager" % "1.2.0"
    val pgp = "com.jsuereth" % "sbt-pgp" % "1.0.1"
    val plugin = "com.typesafe.play" % "sbt-plugin" % "2.5.15"
    val scalafmt = "com.lucidchart" % "sbt-scalafmt-coursier" % "1.7"
    val scoverage = "org.scoverage" % "sbt-scoverage" % "1.5.0"
    val wartRemover = "org.wartremover" % "sbt-wartremover" % "2.1.1"
    val web = "com.typesafe.sbt" % "sbt-web" % "1.3.0"
  }
}
