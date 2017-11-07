// https://github.com/coursier/coursier/issues/450
import coursier.Keys._

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
excludeDependencies += ExclusionRule("org.slf4j", "slf4j-simple")

ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

// don't forget to update these in the project/project/plugins.sbt
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC12")
classpathTypes += "maven-plugin"
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.14")
scalafmtOnCompile := true // for the project/*.scala files

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
