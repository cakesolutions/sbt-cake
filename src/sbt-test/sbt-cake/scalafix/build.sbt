import net.cakesolutions.CakePlatformDependencies

name in ThisBuild := "simple"

scalafmtOnCompile := true

// disable Fatal warnings
scalacOptions in Compile :=
  (scalacOptions in Compile).value.filterNot(_ == "-Xfatal-warnings")
