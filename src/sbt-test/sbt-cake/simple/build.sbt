import net.cakesolutions.CakePlatformDependencies

name in ThisBuild := "simple"

scalafmtOnCompile := true

val logging = project
  .enablePlugins(BuildInfoPlugin)

val engine = project
  .enablePlugins(DockerPlugin, AshScriptPlugin)
  .settings(pipelineStages := Seq(digest, gzip))
  .dependsOn(logging)

val performance = project.enableIntegrationTests
  .enablePlugins(DockerPlugin, AshScriptPlugin)
  .settings(
    libraryDependencies ++= Seq(
      CakePlatformDependencies.Gatling.app,
      CakePlatformDependencies.Gatling.highcharts,
      CakePlatformDependencies.Gatling.testkit,
      CakePlatformDependencies.Gatling.http
    )
  )
