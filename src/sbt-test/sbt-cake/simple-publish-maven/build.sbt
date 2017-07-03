// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import net.cakesolutions.CakePublishMavenPluginKeys._

name in ThisBuild := "simple-publish-maven"

enablePlugins(CakeBuildPlugin, CakePublishMavenPlugin)

snapshotRepositoryResolver :=
  Some(Resolver.file("file", new File(s"${target.value}/snapshot")))

repositoryResolver :=
  Some(Resolver.file("file", new File(s"${target.value}/release")))
