// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import java.time.{Clock, ZonedDateTime}
import java.time.format.DateTimeFormatter

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import sbt._
import sbt.Keys._

/**
  * Plugin for automatically generating release notes in issue management
  * software (e.g. Jira). In order for this to function correctly, we follow the
  * convention of ensuring that all merged commit messages are prefixed with the
  * ticket number (e.g. "PROJECT-124: xxxx"). If this convention is adhered to,
  * then this plugin will extract all issues since the last tagged release and
  * use these to create a release within the issue management project.
  */
object ReleaseNotesPlugin extends AutoPlugin {
  import CakePublishMavenPluginKeys._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def requires: Plugins =
    CakePublishMavenPlugin && CakeBuildInfoPlugin

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override def trigger: PluginTrigger = noTrigger

  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = ReleaseNotesPluginKeys
  import autoImport._

  /** @see http://www.scala-sbt.org/0.13/api/index.html#sbt.package */
  override val projectSettings: Seq[Setting[_]] = Seq(
    versionControlUrl := None,
    issueManagementUrl := None,
    issueManagementProject := None,
    issuePattern := """(\w+-\d+)""".r,
    CakeBuildInfoKeys.externalBuildTools += (
      "curl --version",
      "`curl` command should be installed and PATH accessible"
    ),
    publishReleaseNotes := Defaults.publishReleaseNotes.value,
    releaseProcess :=
      checkReleaseNoteSettings +: releaseProcess.value :+ publishReleaseNotes
  )

  private object Defaults {
    val checkReleaseNoteSettings: Def.Initialize[Task[Unit]] = Def.taskDyn {
      val logger = Keys.streams.value.log
      if (issueManagementUrl.value.isEmpty) {
        logger.info(
          "No issue management (e.g. Jira) URL defined: " +
            "skip publishing release notes to issue management system"
        )
      }
      if (issueManagementProject.value.isEmpty) {
        logger.info(
          "No issue management (e.g. to Jira) project defined: " +
            "skip publishing release notes to issue management system"
        )
      }
      if (versionControlUrl.value.isEmpty) {
        logger.info(
          "No version control (e.g. Github) URL defined: " +
            "skip publishing release notes to version control system"
        )
      }

      Def.task(())
    }

    val publishReleaseNotes: Def.Initialize[Task[Unit]] = Def.taskDyn {
      val logger = Keys.streams.value.log
      if (isDynVerSnapshot.value) {
        logger.info(
          s"Skipping publishing release notes of SNAPSHOT project ${name.value}"
        )
        Def.task(())
      } else {
        logger.info(
          s"Publishing release notes of project ${name.value} ${version.value}"
        )
        val project = issueManagementProject.value.getOrElse("")
        val currentVersion = version.value
        val gitChanges = {
          val getLastTwoTags =
            "git for-each-ref --sort=-taggerdate --format \"%(refname)\" " +
              "--count=2 refs/tags"
          val lastTwoTags =
            Try(
              getLastTwoTags.!!.split("\n").toList
              // Ensure whitespace and string quotes are removed
                .map(_.trim.filterNot(_ == '"'))
                .collect {
                  case fullTag: String if fullTag.startsWith("refs/tags/v") =>
                    fullTag.drop("refs/tags/v".length)
                }
            )

          assume(
            lastTwoTags.isSuccess,
            "Failed to extract tag information from the git repository!"
          )

          lastTwoTags.get match {
            case List() =>
              throw new AssertionError(
                "No versioned tags found: unable to generate release version " +
                  "notes!"
              )
            case List(currentTag) =>
              assume(
                currentTag == currentVersion,
                s"Version tag of $currentTag does not match the current " +
                  s"version of $currentVersion"
              )
              ""
            case List(currentTag, lastTag) =>
              assume(
                currentTag == currentVersion,
                s"Version tag of $currentTag does not match the current " +
                  s"version of $currentVersion"
              )
              s"v$lastTag...v$currentTag"
            case _: List[String] =>
              throw new AssertionError(
                "Impossible scenario: multiple version tags found " +
                  "- unable to generate release version notes!"
              )
          }
        }
        val (issueNumbers, issues) = {
          val issueListCommand =
            s"""git log --pretty=oneline --pretty=format:"%s" $gitChanges"""
          val issueList =
            Try {
              issueListCommand.!!.split("\n").toList
                .map(_.trim.replaceAll("\"", ""))
            }
          val issueNumberList = issueList.flatMap( messages =>
            Try {
              messages
                .flatMap { message =>
                  logger.info(s"- $message")
                  issuePattern.value
                    .findAllIn(message)
                    .matchData
                    .map(_.group(1))
                }
            }
          )
          assume(
            issueNumberList.isSuccess,
            "Failed to extract version tag information for this release from " +
              "the git repository!"
          )
          assume(
            issueNumberList.get.nonEmpty,
            "At least one version tag should exist in the git repository!"
          )
          (issueNumberList.get, issueList.get)
        }
        if (issueManagementUrl.value.isEmpty ||
          issueManagementProject.value.isEmpty) {
          logger.info(
            "Skip publishing release notes to issue management system"
          )
        } else {
          val result = for {
            releaseId <- createJiraRelease(
              version.value,
              project,
              issueManagementUrl.value
            )
            _ = logger.info(s"Created release $releaseId")
            _ <- associateIssuesWithJiraRelease(
              version.value,
              issueNumbers,
              issueManagementUrl.value
            )
            _ = logger.info(s"Associating issues with release $releaseId")
            _ <- closeJiraRelease(releaseId, project, issueManagementUrl.value)
            _ = logger.info(s"Closed the release $releaseId")
          } yield ()

          if (result.isFailure) {
            throw new AssertionError(
              "Failed to publish release notes to " +
                issueManagementUrl.value.getOrElse(""),
              result.failed.get
            )
          }

          logger.info(
            "Completed publishing release notes to " +
              issueManagementUrl.value.getOrElse("")
          )
        }
        if (versionControlUrl.value.isEmpty) {
          logger.info(
            "Skip publishing release notes to version control system"
          )
        } else {
          val result = for {
            _ <- createGithubRelease(
              version.value,
              issues,
              versionControlUrl.value
            )
            _ = logger.info(s"Created release ${version.value}")
          } yield ()

          if (result.isFailure) {
            throw new AssertionError(
              "Failed to publish release notes to " +
                versionControlUrl.value.getOrElse(""),
              result.failed.get
            )
          }

          logger.info(
            "Completed publishing release notes to " +
              versionControlUrl.value.getOrElse("")
          )
        }
        Def.task(())
      }
    }

    private def jiraAuthHeader: Seq[String] = {
      val jiraAuth = sys.env.get("JIRA_AUTH_TOKEN").map { token =>
        BaseEncoding.base64().encode(token.getBytes(Charsets.UTF_8))
      }
      if (jiraAuth.isDefined) {
        Seq("-H", s"Authorization: Basic ${jiraAuth.get}")
      } else {
        Seq.empty
      }
    }

    private def githubAuthHeader: Seq[String] = {
      val oAuth = sys.env.get("GITHUB_AUTH_TOKEN")
      oAuth.toSeq.flatMap(token => Seq("-H", s"Authorization: token $token"))
    }

    // scalastyle:off magic.number
    private def httpClient(
      method: String,
      path: String,
      json: String,
      targetUrl: Option[URL],
      authHeader: Seq[String],
      timeout: Int = 60
    ): Try[String] = {
      val targetUrlStr = targetUrl.fold("")(_.toString)

      val jsonData = json.replaceAll("\n", "")
      val requestCmd = Seq(
        "curl",
        "-X",
        method,
        "-s",
        "-w",
        "HTTPSTATUS:%{http_code}",
        "-d",
        jsonData
      ) ++
        authHeader ++
        Seq(
          "-H",
          "Content-Type: application/json",
          "--max-time",
          timeout.toString,
          s"$targetUrlStr$path"
        )

      Try(requestCmd.!!.trim).flatMap { response =>
        // We eliminate transient HTTP connection timeouts (i.e. 000) from
        // the response
        val responseParts =
          response.replaceAll("HTTPSTATUS:000", "").split("HTTPSTATUS:")
        assume(
          responseParts.length >= 2,
          s"Failed to extract a body and status code from:\n $response"
        )
        val responseBody = responseParts.head.trim
        val statusCodeStr = responseParts.last.trim
        val statusCode = Try(statusCodeStr.toInt)

        if (statusCode.isSuccess
            && 200 <= statusCode.get
            && statusCode.get < 300) {
          Success(responseBody)
        } else if (statusCode.isFailure) {
          Failure(
            new RuntimeException(
              s"$targetUrlStr returned an unparsable status code of " +
                s"'$statusCodeStr': $responseBody"
            )
          )
        } else {
          Failure(
            new RuntimeException(
              s"$targetUrlStr returned an invalid status code of " +
                s"$statusCode (expected 2XX): $responseBody"
            )
          )
        }
      }
    }

    // scalastyle:on magic.number

    private def post(
      path: String,
      json: String,
      targetUrl: Option[URL],
      authHeader: Seq[String]
    ): Try[String] = {
      httpClient("POST", path, json, targetUrl, authHeader)
    }

    private def put(
      path: String,
      json: String,
      targetUrl: Option[URL],
      authHeader: Seq[String]
    ): Try[String] = {
      httpClient("PUT", path, json, targetUrl, authHeader)
    }

    private def createJiraRelease(
      version: String,
      project: String,
      jiraUrl: Option[URL]
    ): Try[String] = {
      val releaseDateTime =
        ZonedDateTime
          .now(Clock.systemUTC())
          .format(DateTimeFormatter.ISO_INSTANT)
      val data =
        s"""
          |{
          |  "description": "An automatically versioned release",
          |  "name": "$version",
          |  "archived": false,
          |  "released": false,
          |  "releaseDate": "$releaseDateTime",
          |  "project": "$project"
          |}
        """.stripMargin

      post("/version", data, jiraUrl, jiraAuthHeader)
    }

    private def associateIssuesWithJiraRelease(
      version: String,
      issueList: List[String],
      jiraUrl: Option[URL]
    ): Try[Unit] = {
      issueList.foldRight[Try[Unit]](Success(())) {
        case (issue, Success(_)) =>
          val data =
            s"""
               |{
               |  "update": {
               |    "fixVersions": [{"set":[{"name" : "$version"}]}]
               |  }
               |}
            """.stripMargin

          put(s"/issue/$issue", data, jiraUrl, jiraAuthHeader).map(_ => ())
        case (_, error) =>
          error
      }
    }

    private def closeJiraRelease(
      releaseId: String,
      project: String,
      jiraUrl: Option[URL]
    ): Try[Unit] = {
      val data =
        s"""
          |{
          |  "id": "$releaseId",
          |  "released": true,
          |  "project": "$project"
          |}
        """.stripMargin

      put(s"/version/$releaseId", data, jiraUrl, jiraAuthHeader).map(_ => ())
    }

    private def createGithubRelease(
      version: String,
      commits: List[String],
      githubUrl: Option[URL]
    ): Try[Unit] = {
      val data =
        s"""
           |{
           |  "tag_name": "v$version",
           |  "target_commitish": "master",
           |  "name": "v$version",
           |  "body": "${commits.mkString("\\n")}",
           |  "draft": false,
           |  "prerelease": false
           |}
         """.stripMargin

      post("/releases", data, githubUrl, githubAuthHeader).map(_ => ())
    }
  }
}

/**
  * Library dependency keys that will be auto-imported when this plugin is
  * enabled on a project.
  */
object ReleaseNotesPluginKeys {

  /**
    * Optional URL pointing to the version control REST APIs.
    */
  val versionControlUrl: SettingKey[Option[URL]] =
    settingKey(
      "Optional URL pointing to the issue management system (e.g. Jira) for " +
        "the publishing of release notes"
    )

  /**
    * Optional URL pointing to the issue management REST APIs.
    */
  val issueManagementUrl: SettingKey[Option[URL]] =
    settingKey(
      "Optional URL pointing to the issue management system (e.g. Jira) for " +
        "the publishing of release notes"
    )

  /**
    * Optional project name. This is the name of the project within the issue
    * management server.
    */
  val issueManagementProject: SettingKey[Option[String]] =
    settingKey(
      "Optional URL pointing to the issue management system (e.g. Jira) for " +
        "the publishing of release notes"
    )

  val issuePattern: SettingKey[Regex] =
    settingKey(
      "Regex pattern identifying issue numbers (e.g. in Jira: PRJ-123) " +
        """defaults to "(\w+-\d+)""""
    )

  /**
    * Task that ensures all preconditions for using this plugin are satisfied.
    */
  val checkReleaseNoteSettings: TaskKey[Unit] =
    taskKey(
      "Ensure that all preconditions for using this plugin are satisfied"
    )

  /**
    * Task that creates the issue management release, adds issues to that
    * release and then closes the release.
    */
  val publishReleaseNotes: TaskKey[Unit] =
    taskKey(
      "Publish release notes of the currently tagged version to the issue " +
        "manager"
    )
}
