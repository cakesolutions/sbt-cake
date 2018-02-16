// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright: 2016 EPFL
// License: http://opensource.org/licenses/BSD-3-Clause
package net.cakesolutions

import scala.util.Try

import org.apache.ivy.core.module.descriptor.MDArtifact
import sbt.Keys._
import sbt._

/**
  * Plugin for publishing artefacts (by default, configured for use by Cake
  * Solution Limited artifacts).
  */
object CakePublishMavenPlugin extends AutoPlugin {
  import CakeDynVerPlugin.{autoImport => DynVer}

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = CakeDynVerPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakePublishMavenPluginKeys
  import autoImport._

  // Derived from https://github.com/scalacenter/sbt-release-early
  private val stableDef = new sbt.TaskSequential {}

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    },
    snapshotRepositoryResolver := None,
    repositoryResolver := None,
    Defaults.addPublishingCredentials(),
    Defaults.artifactPublishingUrl(),
    publishConfiguration := Defaults.publishConfiguration.value,
    checkSnapshotDependencies := Defaults.checkSnapshotDependencies.value,
    checkForCleanRepository := Defaults.checkForCleanRepository.value,
    createRelease := Defaults.createRelease.value,
    releaseProcess := Seq(
      checkForCleanRepository,
      DynVer.dynverAssertVersion,
      checkSnapshotDependencies,
      Keys.publish
    )
  )

  private object Defaults {

    def addPublishingCredentials(): Setting[Task[Seq[Credentials]]] = {
      credentials ++= {
        if ((Path.userHome / ".sbt" / "0.13" / ".credentials").exists()) {
          Seq(Credentials(Path.userHome / ".sbt" / "0.13" / ".credentials"))
        } else {
          Nil
        }
      }
    }

    // Working with resolvers rather than URLs simplifies plugin testing
    def artifactPublishingUrl(): Setting[Option[Resolver]] = {
      publishTo := {
        if (noArtifactToPublish.value) {
          None
        } else if (isDynVerSnapshot.value) {
          snapshotRepositoryResolver.value
        } else {
          repositoryResolver.value
        }
      }
    }

    // Ensures that publishing steps are idempotent without generating errors
    def publishConfiguration: Def.Initialize[Task[PublishConfiguration]] =
      Def.task {
        val config = Keys.publishConfiguration.value

        import config._

        val logger = Keys.streams.value.log
        val cross =
          CrossVersion(
            crossVersion.value,
            scalaVersion.value,
            scalaBinaryVersion.value
          ).getOrElse(identity[String] _)
        val unpublished =
          ivyModule.value.withModule(logger) {
            case (ivy, moduleDescriptor, _) =>
              artifacts.filterNot {
                case (a, file) =>
                  val ivyArtifact =
                    new MDArtifact(
                      moduleDescriptor,
                      cross(a.name),
                      a.`type`,
                      a.extension
                    )
                  val published =
                    ivy.getSettings
                      .getResolver(resolverName)
                      .exists(ivyArtifact)

                  if (published) {
                    logger.warn(
                      "Skipping duplicate publication of published artifact: " +
                        file.name
                    )
                  }
                  published
              }
          }

        new PublishConfiguration(
          ivyFile,
          resolverName,
          unpublished,
          checksums,
          logging,
          overwrite
        )
      }

    // Derived from https://github.com/scalacenter/sbt-release-early
    def checkSnapshotDependencies: Def.Initialize[Task[Unit]] =
      Def.task {
        val logger = Keys.streams.value.log
        logger.info(
          s"Checking that ${name.value} has no SNAPSHOT dependencies"
        )
        val managedClasspath = (Keys.managedClasspath in sbt.Runtime).value
        val moduleIds = managedClasspath.flatMap(_.get(Keys.moduleID.key))
        val snapshots = moduleIds.filter(
          m => m.isChanging || m.revision.endsWith("-SNAPSHOT")
        )

        require(
          snapshots.isEmpty,
          s"""
             |Aborting release process for ${name.value}. Snapshot dependencies
             |have been detected:
             |${snapshots.mkString("\t", "\n", "")}
             |
             |Releasing artifacts that depend on snapshots produces
             |non-deterministic behaviour.
           """.stripMargin
        )
      }

    def checkForCleanRepository: Def.Initialize[Task[Unit]] =
      Def.task {
        val logger = Keys.streams.value.log
        logger.info(s"Checking that ${name.value} is a clean git repository")
        val gitStatus = Try("git status --porcelain".!!.trim)

        require(
          gitStatus.isSuccess && gitStatus.getOrElse("").isEmpty,
          s"Aborting release process for ${name.value}. Project should be a " +
            "clean git repository."
        )
      }

    // Derived from https://github.com/scalacenter/sbt-release-early
    def createRelease: Def.Initialize[Task[Unit]] =
      Def.taskDyn {
        val steps = releaseProcess.value
        // Return task with unit value at the end
        val initializedSteps = steps.map(_.toTask)
        Def.taskDyn {
          stableDef.sequential(initializedSteps, Def.task(()))
        }
      }
  }
}

/**
  * Library dependency keys that will be auto-imported when this plugin is
  * enabled on a project.
  */
object CakePublishMavenPluginKeys {
  import CakeDynVerPlugin.{autoImport => DynVer}

  /**
    * Setting that returns true precisely when the current (dynamic) version is
    * determined to be a SNAPSHOT release.
    *
    * Derived from https://github.com/scalacenter/sbt-release-early
    */
  val isDynVerSnapshot: Def.Initialize[Boolean] = Def.setting {
    val defaultValue = Keys.isSnapshot.value
    val isStable = DynVer.dynverGitDescribeOutput.value.map { info =>
      info.ref.value.startsWith(DynVer.dynVerPattern.value.tagPrefix) &&
      (info.commitSuffix.distance <= 0 || info.commitSuffix.sha.isEmpty)
    }
    val isNewSnapshot = isStable.map(stable => !stable || defaultValue)
    // Return previous snapshot definition in case user has overridden version
    isNewSnapshot.getOrElse(defaultValue)
  }

  /**
    * Setting that returns true if there are *no* artifacts to be published.
    *
    * Derived from https://github.com/scalacenter/sbt-release-early
    */
  val noArtifactToPublish: Def.Initialize[Boolean] = Def.setting {
    import Keys.publishArtifact

    !(
      publishArtifact.value ||
        publishArtifact.in(sbt.Compile).value ||
        publishArtifact.in(sbt.Test).value
    )
  }

  /**
    * Setting defining the resolver for the repository to which SNAPSHOT
    * artifacts will be published. Requires client code to define this setting.
    */
  val snapshotRepositoryResolver: SettingKey[Option[Resolver]] =
    settingKey(
      "Resolver defining the repository to which SNAPSHOT artifacts will be " +
        "published"
    )

  /**
    * Setting defining the resolver for the repository to which artifacts will
    * be published. Requires client code to define this setting.
    */
  val repositoryResolver: SettingKey[Option[Resolver]] =
    settingKey(
      "Resolver defining the repository to which artifacts will be published"
    )

  /**
    * Setting that defines the tasks to be performed in order to cut a release.
    */
  val releaseProcess: SettingKey[Seq[TaskKey[Unit]]] =
    settingKey("Sequence of tasks defining the release process")

  /**
    * Task that determines if any artifact dependencies are SNAPSHOTs. If
    * SNAPSHOTs are found, then the SBT build will fail.
    */
  val checkSnapshotDependencies: TaskKey[Unit] =
    taskKey("Check that no artifact dependencies are SNAPSHOTs")

  /**
    * Task that determines if the current project Git repository is clean.
    */
  val checkForCleanRepository: TaskKey[Unit] =
    taskKey("Check that project repository is clean")

  /**
    * Task for cutting a new release. Repository code should be manually tagged
    * with next release version ID (i.e. should start with a `v`) prior to
    * calling this task.
    */
  val createRelease: TaskKey[Unit] =
    taskKey("Publish a release of the currently tagged version")
}
