# Overview

`sbt-cake` is an SBT plugin that should be used within all projects at Cake Solutions Limited.

It defines base settings for continuous integration and continuous deployment automation, coding standard enforcement
and library versioning. The aim here is to supply and recommend a consistent set of good practices for use on all
projects.

This plugin is intended to be used as an accelerator for early project start up. As a project matures, it is expected
that teams will migrate plugin functionality into their respective projects. For example, as a project moves towards
production, then it is expected that teams should control their own library dependencies and so should eventually
migrate away from the use of the `CakePlatformPlugin`.

Note that by incorporating the `sbt-cake` plugin within your local project you automatically enable the
[neo-sbt-scalafmt](http://scalameta.org/scalafmt/) plugin. This plugin provides the following additional SBT tasks:
* `scalafmt` - rewrites project `src/main` code to match style rules present in a `.scalafmt.conf` file
* `test:scalafmt` - rewrites project `src/test` code to match style rules present in a `.scalafmt.conf` file
* `scalafmt::test` - tests that project `src/main` code matches style rules present in a `.scalafmt.conf` file (no
  changes are made to the project code)
* and `test:scalafmt::test` - tests that project `src/test` code matches style rules present in a `.scalafmt.conf` file
  (no changes are made to the project code).

Moreover, note that projects must supply their own `.scalafmt.conf` file. If projects are generated using Cake
[giter8](http://www.foundweekends.org/giter8/) project templates, then this will be done automatically for you.

In addition to this, a number of `sbt-cake` supplied plugins may be configured and enabled (see below for more
information).

# Environment Variables for Configuring the Build Plugin

The `CakePlatformPlugin` may be configured in different build environments by setting environment variables. The setting
of these environment variables allows for faster builds and the investigation of testing issues. These settings are
typically used in continuous integration environments (e.g. Jenkins).

## `CI`

Setting of this environment variable indicates that the build code is running within a continuous integration (CI)
environment (e.g. Jenkins).

Used by `CakePlatformPlugin` and `CakeStandardsPlugin`.

## `DOCKER_COMPOSE_PROJECT_NAME`

Setting of this environment variable defines the project name for Docker Compose. This environment variable is typically
only set within a continuous integration (CI) environment - e.g. when the environment might be shared and some form of
test isolation is required.

Used by `CakeDockerComposePlugin`.

## `DOCKER_REPOSITORY`

Setting of this environment variable defines which Docker repository that images will be published to. This environment
variable is typically only set within a continuous integration (CI) environment when the `publish` task is ran.

Used by `CakeDockerPlugin`.

## `GC_LOGGING`

Setting of this environment variable indicates that the JVM Garbage Collection analyser will be used as part of your
testing or analysis framework. See [dev tools for JVM memory problems](https://github.com/fommil/lions-share#library)
for more details.

Used by `CakePlatformPlugin`, `CakeStandardsPlugin` and `CakeBuildPlugin`.

## `JIRA_AUTH_TOKEN`

Plaintext authentication token used to authenticate against Jira.

Used by `ReleaseNotesPlugin`.

## `SBT_TASK_LIMIT`

When defined, holds a positive integer value. This is used to constrain the number of concurrently executing SBT tasks
to be `SBT_TASK_LIMIT`. If `SBT_TASK_LIMIT` is not set or is invalid, then (by default) a value of 4 is chosen.

Used by `CakePlatformPlugin`, `CakeStandardsPlugin` and `CakeBuildPlugin`.

## `SBT_VOLATILE_TARGET`

When defined, sets the base directory for SBT's `target` setting. This is typically used to relocate the SBT build
`target` directory and the JVM's temporary directory (i.e. the `java.io.tmpdir` property). For example, SBT builds can
be speeded up by ensuring that `target` is located on a RAM disk in continuous integration (e.g. Jenkins).

Used by `CakePlatformPlugin`, `CakeStandardsPlugin` and `CakeBuildPlugin`.

# Build Plugin Functionality

By enabling each of this libraries defined plugins within your project, you enable their respective SBT functionality.

Settings within these plugins can be modified or overridden within local project SBT files (see below for useful
examples).

## `CakeBuildInfoPlugin`: Standard Build Information for Runtime Code

Plugin requirements: `BuildInfoPlugin`

This plugin is auto-loaded if all requirements are loaded.

Assumes that `git` is installed. Used to configure and load the `BuildInfo` object at runtime. This object
contains information regarding the build such as the git commit hash, etc.

### Plugin Configuration

The following configuration settings can be modified in projects that enable this plugin:
* `generalInfo` - generic set of labelled values used, for example, to label jar manifests and Docker containers
* `dockerInfo` - Docker specific labelled values used, for example, to label Docker containers
* `externalBuildTools` - list of command/error message pairs that will be checked by the task `checkExternalBuildTools`.

### SBT Tasks

The following SBT tasks are enabled:
* `checkExternalBuildTools` - checks that all commands in `externalBuildTools` run without generating a non-zero exit
  code.

In continuous integration (CI) environments, the SBT task `checkExternalBuildTools` should be ran by build scripts to
ensure that all required build tools (e.g. `git`, `docker`, etc.) are installed.

## `CakeBuildPlugin`: Standard Build and Library Dependencies

Plugin requirements: `DynVerPlugin`

This plugin is auto-loaded if all requirements are loaded.

Enabling this plugin in a project provides:
* standard build and compiler settings
* version setting using git via the [SBT Dynamic Versioning plugin](https://github.com/dwijnand/sbt-dynver).

All projects should enable this plugin (either directly or indirectly).

### Plugin Configuration

See the plugin's source code and [Custom version string](https://github.com/dwijnand/sbt-dynver#custom-version-string)
to understand the range of build settings that may be modified.

### SBT Tasks

See [Tasks](https://github.com/dwijnand/sbt-dynver#tasks) for information on the
[SBT Dynamic Versioning plugin](https://github.com/dwijnand/sbt-dynver) tasks that this plugin includes.

## `CakeDockerComposePlugin`: Docker Compose Build Helper

Plugin requirements: `CakeDockerPlugin` and `DockerPlugin`

This plugin needs to be explicitly enabled in order to be loaded.

Enabling this plugin is a project provides:
* build definable setting for a docker-compose project file
* helper SBT tasks for manipulating and using Docker compose services.

Any project that uses Docker and Docker Compose for its integration tests should enable this plugin. Setting the
environment variable `DOCKER_COMPOSE_PROJECT_NAME` allows the default Docker Compose project name to be overridden.

### Plugin Configuration

The following configuration settings can be modified in projects that enable this plugin:
* `dockerComposeFile` (`docker/<project module>.yml` by default) - defines the YAML file to be used by Docker Compose
  commands
* `dockerComposeEnvVars` (empty by default) - allows environment variables to be added to the docker-compose process
  environment. This allows Docker image versions and service ports to be defined using data present in the SBT build
  environment.
* `dockerComposeUpLaunchStyle` (`-d` by default) - defines the style for the Docker Compose `up` command (can be used to
  specify `--abort-on-container-exit` or `--exit-code-from SERVICE`)
* `dockerComposeUpExtras` (`Seq("--remove-orphans")` by default) - defines extra command line arguments that will be
  appended to the `dockerComposeUp` task
* `dockerComposeDownExtras` (`Seq("--remove-orphans")` by default) - defines extra command line arguments that will be
  appended to the `dockerComposeDown` task.

### SBT Tasks

The following SBT tasks are enabled:
* `dockerComposeConfigCheck` - validates correctness of the docker-compose YAML configuration
* `dockerComposeDown` - running docker-compose services will be stopped and removed
* `dockerComposeImageTask` - built docker images will be locally published
* `dockerComposeUp` - Docker Compose services (as defined in a Docker Compose YAML file) will be launched
* `dockerRemove` - Docker Compose service images will be force removed.

## `CakeDockerPlugin`: Docker Native Packager

Plugin requirements: `CakeBuildInfoPlugin` and `DockerPlugin`

This plugin needs to be explicitly enabled in order to be loaded.

Enabling this plugin in a project configures how Docker containers may be built. See
[Docker Plugin](http://www.scala-sbt.org/sbt-native-packager/formats/docker.html) for more information. This plugin is
typically used alongside the `CakeJavaAppPlugin`. 

### Plugin Configuration

Docker container version number may be specified at the SBT command line by setting the system property `tag`.

The following configuration settings can be modified in projects that enable this plugin:
* `dockerRepository` (set to the value of `DOCKER_REPOSITORY` or the project name) - used to define where the container
  will be published

### SBT Tasks

The expected suite of SBT Native Packager Docker tasks are enabled (e.g. `docker:stage`, `docker:publishLocal` and
`docker:publish`).

## `CakeDynVerPlugin`: Dynamic Versioning Plugin

Plugin requirements: `JvmPlugin`

This plugin is auto-loaded if all requirements are loaded.

Enabling this plugin in a project allows for dynamic versioning to be flexibly altered. Its implementation is based on
the SBT DynVerPlugin, however this implementation allows version tags to have a definable prefix.

For most projects, customisation of the version tags should be a case of modifying the `mkVersion` setting. For more
involved versioning strategies (e.g. with project specific tag prefixes), then it may be necessary to also modify the
`dynVerPattern` setting. An example of how this may be done may be found in the `sbt-cake/custom-publish-maven` SBT
scripted plugin tests.

### Plugin Configuration

The following settings keys are the main way in which the plugin is configured:
* `dynVerPattern`: data for configuring git tag version prefixing
* `mkVersion`: maps a `Option[GitDescribeOutput]` value to a version string

The following settings keys are intended for application reading:
* `dynver`: the dynamic versioning string
* `dynverGitDescribeOutput`: parsed form of the git describe output
* `dynverCurrentDate`: current UTC date for use in versioning

The following settings keys should not be generally altered and exist mainly for internal plugin usage:
* `dynVerPlugin`: holds the dynamic versioning plugin code configured with tag prefixing data

### SBT Tasks

* `dynverCheckVersion`: checks if the `dynver` and `version` values matches or not
* `dynverAssertVersion`: asserts that `dynver` and `version` values matches.

## `CakeJavaAppPlugin`: Java Application Server Packaging

Plugin requirements: `JavaServerAppPackaging`

This plugin needs to be explicitly enabled in order to be loaded.

Enabling this plugin in a project configures how JVM applications may be built. See
[Java Application Archetype](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/) for more information.

Should your Docker containers use (for example) an Alpine base image, then it will be necessary to also enable the
`AshScriptPlugin` as a dependency in your project code.

### Plugin Configuration

The following configuration settings can be modified in projects that enable this plugin:
* `mainClass in Compile ` - used to define the `Main` class that will be launched when running the jar file

### SBT Tasks

The expected suite of SBT Native Packager Java application tasks are enabled (e.g. `stage`, `universal:packageBin`,
`debian:packageBin` and `docker:publishLocal`).

## `CakeDockerVersionPlugin`: Docker and Docker Compose Version Checking

Plugin requirements: `CakeDockerComposePlugin`

Enabling this plugin in a project provides setting minimum required `docker` and `docker-compose` versions and checking
capability of either minimum versions are installed in the running environment or not.

### Plugin Configuration

The following configuration settings can be modified in projects that enable this plugin:
* `minimumDockerVersion` (`(1, 13)` by default) - minimum docker version is `1.13`.
* `minimumDockerComposeVersion` (`(1, 10)` by default) - minimum docker-compose version is `1.10`.

### SBT Tasks

The following SBT tasks are enabled:
* `checkDockerVersion` - checks if docker version of machine is above the `minimumDockerVersion`.
* `checkDockerComposeVersion` - checks if docker-compose version of machine is above the `minimumDockerComposeVersion`. 


## `CakeDockerHealthPlugin`: Health Checking of Containers in Docker Compose Scope

Plugin requirements: `CakeDockerComposePlugin`

Enabling this plugin in a project provides dumping the logs and health checking of the containers 
defined in the `dockerComposeFile` file in the `CakeDockerComposePlugin`. 

### Plugin Configuration

There is no special configuration provided by this plugin.

### SBT Tasks

SBT tasks of that plugin requires a running docker-compose stack.
The following SBT tasks are enabled:
* `dumpContainersLogs` - dumping logs of each container in the docker-compose scope.
* `checkContainersHealth` - checking the health status of each container in the docker-compose scope.
 All containers who have a health check definition should be healthy. 
 If a container does not have a health check instruction, it will be ignored.
 You can check that [post](https://blog.couchbase.com/docker-health-check-keeping-containers-healthy/) for details of docker health check feature.

## `CakeTestRunnerPlugin`: Integration and Performance Test Lifecycle within Docker Stack

Plugin requirements: `CakeDockerComposePlugin`

Enabling this plugin in a project provides the functionality of running integration and performance tests
within automatically managed docker fleet (stack). This plugin provides resource cleaning in case of failure or success
of the tests. The lifecycle is consisting of following steps:

* docker and docker-compose version checks
* docker-compose up
* health check of containers in docker-compose scope
* running tests
* dumping logs of containers
* docker-compose down
* docker remove containers in docker-compose scope


Integration tests depend on unit tests.
Performance tests depend on integration tests and using GatlingPlugin and its GatlingIt (integration) tests settings.
GatlingIt looks for performance tests under `it` directory.

### Plugin Configuration

In order to wait enough for containers to reach to the healthy status,
the following configuration settings can be modified in projects that enable this plugin:

* `healthCheckRetryCount` (5 by default) - defines how much times plugin needs in total to retry 
all containers reach to the healthy status
* `healthCheckIntervalInSeconds` (5 seconds by default) - defines the rechecking interval for containers' health again. 

### SBT Tasks

The following SBT tasks are enabled:
* `integrationTests` - runs integration tests within all docker operations and steps
* `performanceTests` - runs performance tests within all docker operations and steps 

## `CakePublishMavenPlugin`: Artifact Publishing

Plugin requirements: `DynVerPlugin`

This plugin needs to be explicitly enabled in order to be loaded.

Enabling this plugin provides support for dynamic versioning of project code and for tag based release strategies.
However, for mono project repositories, only one versioned set of artifacts may be published with this plugin.

When enabled, this plugin allows for the following release workflows (see sections below for how to configure for this):
* typical SNAPSHOT release workflow is as follows:
```text
# Within a Jenkins/CI environment
# Ensure that the current working tree is clean - otherwise the release will fail!
sbt createRelease
```
* typical production release workflow for version `X.Y.Z` is as follows:
```text
# Within a local developer environment or via a Jenkins/CI trigger
git tag -a vX.Y.Z -m "vX.Y.Z"
git push --tags

# Within a Jenkins/CI environment
# Ensure that the current working tree is clean - otherwise the release will fail!
sbt createRelease
```

### Plugin Configuration

Should it be necessary to authenticate against the publishing repository, then the `credentials` setting can be used to
specify a file holding the authentication credentials. By default, the file `$HOME/.sbt/0.13/.credentials` is used (if
it exists).

Settings for determining where artifacts will be published are as follows:
* `snapshotRepositoryResolver` - location for the publishing of SNAPSHOT artifacts. For example:
```scala
snapshotRepositoryResolver := Some("Snapshot Artifactory" at "http://artifactory.cakesolutions.net/snapshot")
```
* `repositoryResolver` - location for the publishing of production artifacts. For example:
```scala
repositoryResolver := Some("Nexus" at "http://nexus.cakesolutions.net")
```

A typical project's `Jenkinsfile` would then be updated to have the following additional stage:
```text
stage('Publish') {
  steps {
    ansiColor('xterm') {
      sh "sbt createRelease"
    }
  }
}
```

The release workflow may be further customised by modifying the settings key `releaseProcess`. By default, the current
working tree is checked to be clean and project dependencies are checked to be free of SNAPSHOTs, only then will
artifacts be published.

### SBT Tasks

The main SBT task that continuous integration (CI) environments should use for publishing artifacts to Maven based
repositories (e.g. Artifactory or Nexus) is `createRelease`.

The following are internal SBT plugin tasks and are not normally expected to be modified by developers:
* `checkSnapshotDependencies` - assertion based check to ensure that no SNAPSHOT dependencies exist for the code to be
  published
* `checkForCleanRepository` - assertion based check to ensure that the current repository working tree is clean.

## `CakeStandardsPlugin`: Scala Compiler Options, Linter, Wartremover and Scala Format

Plugin requirements: `CakeBuildPlugin` and `DynVerPlugin`

This plugin is auto-loaded if all requirements are loaded.

Enabling this plugin in a project configures [wartremover](https://github.com/wartremover/wartremover), along with a
standard suite of compiler compatibility flags.

### Plugin Configuration

Configuration of this plugin is not generally recommended. To aid local development::
* [wartremover](https://github.com/wartremover/wartremover) logs issues as warnings and not errors (in CI environments
  these will be *errors*)
* warnings are treated as warnings (in CI environments these will be *errors*).

Should one wish to use [linter](https://github.com/HairyFotr/linter), then the following code will need to be added to
project build settings (see [linter configuration](https://github.com/HairyFotr/linter#enablingdisabling-checks) for
further configuration information):
```scala
addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
```
Currently, [linter](https://github.com/HairyFotr/linter) is not enabled by default since there are reports of it not
being maintained and conflicting with [wartremover](https://github.com/wartremover/wartremover).

### SBT Tasks

No special tasks are enabled for this plugin.

## `ReleaseNotesPlugin`: Publish Release Notes to Project Management Tools

Plugin requirements: `CakePublishMavenPlugin` and `CakeBuildInfoPlugin`

This plugin needs to be explicitly enabled in order to be loaded.

Enabling this plugin in a project allows release notes to be automatically generated and submitted to an issue
management tool such as Jira. This allows the issue management tool to be the single source of truth regarding the
current state of a project.

A commit naming convention is used by this plugin to ensure that the correct issue management tickets for a release may
be identified. The plugin assumes that all tickets for an issue will have commit messages prefixed to match the regular
expression `\w+-\d+`.

Having enabled and configured this plugin, a developer only needs to follow the tag release strategy of the
`CakePublishMavenPlugin` - i.e. run the `createRelease` task. No other special actions are required.

### Plugin Configuration

In order to configure this plugin, the following settings keys need to be defined:
* `issueManagementUrl` - this defines the URL of the issue management server
* `issueManagementProject` - this defines the project within the issue management server that tracks this project's
  issues and against which release notes should be created.

### SBT Tasks

The following SBT internal tasks exist for this plugin:
* `checkReleaseNoteSettings` - used to ensure that all preconditions for using this plugin are satisfied. If this check
  fails, then the release will be terminated. This task is ran as the first step of a `CakePublishMavenPlugin`
  `createRelease` task run.
* `publishReleaseNotes` - creates the issue management release, adds issues to that release and then closes the release.
  This task is ran as the last step of a `CakePublishMavenPlugin` `createRelease` task run.

# Releasing of the `sbt-cake` Plugin to the Sonatype Repository

Currently (and this is temporary - see CO-132), in order to release version `X.Y.Z` of the `sbt-cake` plugin to the
`net.cakesolutions` Sonatype repository, perform the following actions:
```text
export CI=local
git tag -s vX.Y.Z
sbt publishSigned sonatypeRelease
git push --tags
```

In order for releasing to occur, individuals should have the Cake Solutions Sonatype PGP keys setup in their local
developer environments.
