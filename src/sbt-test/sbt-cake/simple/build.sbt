name in ThisBuild := "simple"

val logging = project
  .enablePlugins(BuildInfoPlugin)

val engine = project
  .enablePlay
  .enablePlugins(BuildInfoPlugin, DockerPlugin, AshScriptPlugin)
  .settings(
    pipelineStages := Seq(digest, gzip)
  )
  .dependsOn(logging)

val performance = project
  .enablePlugins(BuildInfoPlugin, DockerPlugin, AshScriptPlugin)
  .settings(
    libraryDependencies ++= deps.Gatling
  )
