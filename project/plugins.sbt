ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("com.fommil" % "sbt-sensible" % "1.2.0")

// 1.8.0 causes https://github.com/sbt/sbt-header/issues/56
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
