// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO

/**
  * Mock Issue Management server - for use during SBT scripted plugin testing.
  *
  * All received endpoint data is written to a endpoint specific files in the
  * build target directory.
  */
object MockIssueManagementServer extends App {

  private implicit val system = ActorSystem("MockIssueManagementServer")
  private implicit val materializer = ActorMaterializer()

  private val fixedReleaseId = "release-42"
  private val mockRoutes: Route =
    path("version") {
      post {
        extractDataBytes { bytes =>
          bytes
            .runWith(FileIO.toPath(new File("post-version.json").toPath))
          complete(fixedReleaseId)
        }
      }
    } ~
      path("version" / fixedReleaseId)  {
        put {
          extractDataBytes { bytes =>
            bytes
              .runWith(FileIO.toPath(new File("put-version.json").toPath))
            complete(OK)
          }
        }
      } ~
      path("issue" / Segment) { issueId =>
        put {
          extractDataBytes { bytes =>
            bytes
              .runWith(FileIO.toPath(new File(s"put-issue-$issueId.json").toPath))
            complete(OK)
          }
        }
      } ~
      { ctx =>
        println(s"Rejected: ${ctx.request}")
        ctx.complete(Forbidden)
      }

  Http().bindAndHandle(mockRoutes, "0.0.0.0", 8080)
}
