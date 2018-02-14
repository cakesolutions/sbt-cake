// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

// Copyright: 2016-2017 Dale Wijnand
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

import scala.util._
import scala.util.matching.Regex

import sbt._
import sbt.Keys._

/**
  * Dynamic versioning plugin
  */
object CakeDynVerPlugin extends AutoPlugin {

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins = plugins.JvmPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = allRequirements

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  object autoImport {

    val dynVerPlugin: SettingKey[DynVerPluginData] =
      settingKey[DynVerPluginData]("")

    val dynVerPattern: SettingKey[DynVerPattern] =
      settingKey[DynVerPattern]("Dynamic versioning pattern configuration")

    val dynver: SettingKey[String] =
      settingKey[String]("The version of your project, from git")

    val dynverCurrentDate: SettingKey[Instant] =
      settingKey[Instant]("The current UTC time instant, for dynver purposes")

    val dynverGitDescribeOutput: SettingKey[Option[GitDescribeOutput]] =
      settingKey[Option[GitDescribeOutput]]("The output from git describe")

    val mkVersion: SettingKey[Option[GitDescribeOutput] => String] =
      settingKey[Option[GitDescribeOutput] => String](
        "Setting defining how to map git describe output to a version string"
      )

    val dynverCheckVersion: TaskKey[Boolean] =
      taskKey[Boolean]("Checks if version and dynver match")

    val dynverAssertVersion: TaskKey[Unit] =
      taskKey[Unit]("Asserts if version and dynver match")
  }
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def buildSettings: Seq[Setting[_]] = {
    Seq(
      dynVerPattern := DynVerPattern.defaults,
      dynVerPlugin := new DynVerPluginData(dynVerPattern.value),
      isSnapshot :=
        dynVerPlugin.value.lift(dynverGitDescribeOutput.value).isSnapshot,
      mkVersion := {
        case None =>
          s"0.0.0-${timestamp(dynverCurrentDate.value)}-SNAPSHOT"

        case Some(out) =>
          (
            out.ref.value.startsWith(dynVerPattern.value.tagPrefix),
            out.commitSuffix.distance
          ) match {
            case (true, 0) =>
              dynVerPlugin.value.lift(out.ref).dropV
            case (true, _) =>
              dynVerPlugin.value.lift(out.ref).dropV +
                s"-${out.commitSuffix.sha}-SNAPSHOT"
            case (false, _) =>
              s"0.0.0-${out.ref.value}-SNAPSHOT"
          }
      },
      dynver := mkVersion.value(dynverGitDescribeOutput.value),
      version := dynver.value,
      dynverCurrentDate := Instant.now(),
      dynverGitDescribeOutput := dynVerPlugin.value.getGitDescribeOutput,
      dynverCheckVersion := (dynver.value == version.value),
      dynverAssertVersion := {
        assert(
          dynverCheckVersion.value,
          s"Version and dynver mismatch - version: ${version.value}, " +
            s"dynver: ${dynver.value}"
        )
      }
    )
  }

  private def timestamp(d: Instant): String =
    DateTimeFormatter
      .ofPattern("yyyyMMdd-HHmm")
      .withZone(ZoneId.of("UTC"))
      .format(d)
}

/**
  * Configurable tag prefixing for the dynamic versioning plugin.
  *
  * @param tagPrefix string prefix used for tagging (e.g. v)
  * @param Tag regular expression for parsing and extracting git tag information
  * @param gitTagMatchPattern blob used to match against git tags
  */
final case class DynVerPattern(
  tagPrefix: String,
  Tag: Regex,
  gitTagMatchPattern: String
)

/**
  * Default settings for dynamic versioning plugin's tag prefixing.
  */
object DynVerPattern {

  val defaults: DynVerPattern = {
    val tagPrefix: String = "v"
    val Tag: Regex = s"""($tagPrefix[0-9][^+]*)""".r
    val gitTagMatchPattern: String = s"$tagPrefix[0-9]*"

    DynVerPattern(
      tagPrefix,
      Tag,
      gitTagMatchPattern
    )
  }
}

/**
  * Core data and functions for dynamic versioning plugin - configured using
  * tag prefixing data.
  *
  * @param dynVerPatternImpl settings for dynamic versioning plugin's tag
  *   prefixing
  */
class DynVerPluginData(dynVerPatternImpl: DynVerPattern) {

  import dynVerPatternImpl._

  /**
    * Structured result from parsing the git describe output.
    *
    * @return None is this is not a git repository. Some with a
    *   parsed/structured git describe output when this is a git repository.
    */
  def getGitDescribeOutput: Option[GitDescribeOutput] = {
    val gitDescribeCmd =
      List(
        "git",
        "describe",
        "--tags",
        "--abbrev=8",
        "--match",
        gitTagMatchPattern,
        "--always",
        "--dirty"
      )

    Try(Process(gitDescribeCmd).!!(NoProcessLogger)).toOption
      .map(_.replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2"))
      .map(parse)
  }

  /**
    * Function to lift certain GitDescribeOutput operations to an Option type.
    *
    * @param _x option type we are lifting functions to
    * @return functions we inject into the option type
    */
  def lift(_x: Option[GitDescribeOutput]): OptGitDescribeOutputOps =
    new OptGitDescribeOutputOps {

      def isSnapshot: Boolean = {
        _x.forall(gitDescribeSnapshot)
      }
    }

  /**
    * Function to lift or inject operations into a GitRef type.
    *
    * @param _x type we are lifting functions to
    * @return functions we inject into the GitRef type
    */
  def lift(_x: GitRef): GitRefOps =
    new GitRefOps {

      def dropV: String = {
        _x.value.replaceAll(s"^$tagPrefix", "")
      }
    }

  private val Distance: Regex = """\+([0-9]+)""".r
  private val Sha: Regex = """([0-9a-f]{8})""".r
  private val CommitSuffix: Regex = s"""($Distance-$Sha)""".r
  private val FromTag: Regex = s"""^$Tag$CommitSuffix?(-dirty)?$$""".r
  private val FromSha: Regex = s"""^$Sha(-dirty)?$$""".r
  private val FromHead: Regex = s"""^HEAD(-dirty)$$""".r

  private def gitDescribeSnapshot(_y: GitDescribeOutput): Boolean =
    _y.dirtySuffix.value.nonEmpty ||
      !_y.ref.value.startsWith(tagPrefix) ||
      _y.commitSuffix.distance > 0

  private def gitDescribeStable(_y: GitDescribeOutput): Boolean =
    _y.dirtySuffix.value.isEmpty

  private def parse(s: String): GitDescribeOutput = {
    s.trim match {
      case FromTag(tag, _, dist, sha, dirty) =>
        parse0(tag, dist, sha, dirty)
      case FromSha(sha, dirty) =>
        parse0(sha, "0", "", dirty)
      case FromHead(dirty) =>
        parse0("HEAD", "0", "", dirty)
    }
  }

  private def parse0(
    ref: String,
    dist: String,
    sha: String,
    dirty: String
  ): GitDescribeOutput = {
    val commit =
      if (Option(dist).isEmpty || Option(sha).isEmpty) {
        GitCommitSuffix(0, "")
      } else {
        GitCommitSuffix(dist.toInt, sha)
      }

    GitDescribeOutput(
      GitRef(ref),
      commit,
      GitDirtySuffix(Option(dirty).getOrElse(""))
    )
  }
}

/**
  * Encodes a git reference
  *
  * @param value git reference
  */
final case class GitRef(value: String)

/**
  * Encodes a git suffix
  *
  * @param distance number of commits since the last tag release
  * @param sha the current commit SHA
  */
final case class GitCommitSuffix(distance: Int, sha: String)

/**
  * Encodes the suffix if the git repository is dirty
  *
  * @param value suffix indicating the repository is dirty (may be empty)
  */
final case class GitDirtySuffix(value: String)

/**
  * Encodes the git describe output
  *
  * @param ref git reference
  * @param commitSuffix git commit
  * @param dirtySuffix git dirty suffix
  */
final case class GitDescribeOutput(
  ref: GitRef,
  commitSuffix: GitCommitSuffix,
  dirtySuffix: GitDirtySuffix
)

/**
  * Operations that will be injected into GitRef
  */
trait GitRefOps {

  /**
    * drop version tag prefix
    *
    * @return version tag without the tag prefix
    */
  def dropV: String
}

/**
  * Operations that will be injected into optional GitDescribeOutput
  */
trait OptGitDescribeOutputOps {

  /**
    * Check if the current git describe encapsulates a SNAPSHOT version or not.
    *
    * @return true if and only if we are a SNAPSHOT version
    */
  def isSnapshot: Boolean
}

private object NoProcessLogger extends ProcessLogger {

  def info(s: => String): Unit = ()

  def error(s: => String): Unit = ()

  def buffer[T](f: => T): T = f
}
