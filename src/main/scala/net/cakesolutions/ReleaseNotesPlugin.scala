// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

package net.cakesolutions

import java.time.{Clock, ZonedDateTime}
import java.time.format.DateTimeFormatter

import scala.util.{Failure, Success, Try}

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
    issueManagementUrl := None,
    issueManagementProject := None,
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
      require(
        issueManagementUrl.value.nonEmpty,
        "Need to define an issue management URL (e.g. to Jira) before we can " +
          "publish release notes!"
      )
      require(
        issueManagementProject.value.nonEmpty,
        "Need to define an issue management (e.g. Jira) project before we " +
          "can publish release notes!"
      )

      Def.task(())
    }

    val publishReleaseNotes: Def.Initialize[Task[Unit]] = Def.taskDyn {
      val logger = Keys.streams.value.log
      if (isDynVerSnapshot.value) {
        logger.info(
          "Skipping publishing of SNAPSHOT project " +
            s"${issueManagementProject.value} release notes " +
            s"for project ${name.value} to ${issueManagementUrl.value}"
        )
        Def.task(())
      } else {
        logger.info(
          s"Publishing project ${issueManagementProject.value} release notes " +
            s"for ${name.value} to ${issueManagementUrl.value}"
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
              s"$lastTag...$currentTag"
            case _: List[String] =>
              throw new AssertionError(
                "Impossible scenarion: multiple version tags found " +
                  "- unable to generate release version notes!"
              )
          }
        }
        val issues = {
          val issueListCommand =
            s"""git log --pretty=oneline --pretty=format:"%s" $gitChanges"""
          val issueList =
            Try {
              issueListCommand.!!.split("\n").toList
                .map(_.trim.replaceAll("\"", ""))
                .filter(_.matches("^\\w+-\\d+:.*$"))
                .map(_.split(":").head)
            }

          assume(
            issueList.isSuccess,
            "Failed to extract version tag information for this release from " +
              "the git repository!"
          )
          assume(
            issueList.get.nonEmpty,
            "At least one version tag should exist in the git repository!"
          )

          issueList.get
        }
        val result = for {
          releaseId <- createJiraRelease(
            version.value,
            project,
            issueManagementUrl.value
          )
          _ = logger.info(s"Created release $releaseId")
          _ <- associateIssuesWithJiraRelease(
            version.value,
            issues,
            issueManagementUrl.value
          )
          _ = logger.info(s"Associating issues with release $releaseId")
          _ <- closeJiraRelease(releaseId, project, issueManagementUrl.value)
          _ = logger.info(s"Closed the release $releaseId")
        } yield ()

        if (result.isFailure) {
          throw new AssertionError(s"Failed to publish release notes: $result")
        }

        logger.info(
          s"Completed publishing release notes to ${issueManagementUrl.value}"
        )

        Def.task(())
      }
    }

    // scalastyle:off magic.number
    private def httpClient(
      method: String,
      path: String,
      json: String,
      jiraUrl: Option[URL],
      timeout: Int = 60
    ): Try[String] = {
      val jiraUrlStr = jiraUrl.fold("")(_.toString)
      val jiraAuth = sys.env.get("JIRA_AUTH_TOKEN").map { token =>
        BaseEncoding.base64().encode(token.getBytes(Charsets.UTF_8))
      }
      val authenticationHeader =
        if (jiraAuth.isDefined) {
          Seq("-H", s"Authorization: Basic ${jiraAuth.get}")
        } else {
          Seq.empty
        }
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
        authenticationHeader ++
        Seq(
          "-H",
          "Content-Type: application/json",
          "--max-time",
          timeout.toString,
          s"$jiraUrlStr$path"
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
              s"$jiraUrlStr returned an unparsable status code of " +
                s"'$statusCodeStr': $responseBody"
            )
          )
        } else {
          Failure(
            new RuntimeException(
              s"$jiraUrlStr returned an invalid status code of " +
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
      jiraUrl: Option[URL]
    ): Try[String] = {
      httpClient("POST", path, json, jiraUrl)
    }

    private def put(
      path: String,
      json: String,
      jiraUrl: Option[URL]
    ): Try[String] = {
      httpClient("PUT", path, json, jiraUrl)
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

      post("/version", data, jiraUrl)
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

          put(s"/issue/$issue", data, jiraUrl).map(_ => ())
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

      put(s"/version/$releaseId", data, jiraUrl).map(_ => ())
    }
  }
}

/**
  * Library dependency keys that will be auto-imported when this plugin is
  * enabled on a project.
  */
object ReleaseNotesPluginKeys {

  /**
    * Optional URL pointing to the issue management REST APIs. This needs to be
    * defined in order to use this plugin.
    */
  val issueManagementUrl: SettingKey[Option[URL]] =
    settingKey(
      "Optional URL pointing to the issue management system (e.g. Jira) for " +
        "the publishing of release notes"
    )

  /**
    * Optional project name. This is the name of the project within the issue
    * management server. This needs to be defined in order to use this plugin.
    */
  val issueManagementProject: SettingKey[Option[String]] =
    settingKey(
      "Optional URL pointing to the issue management system (e.g. Jira) for " +
        "the publishing of release notes"
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
