// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

/**
  * Mock server - for use during SBT scripted plugin testing.
  */
@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.NonUnitStatements"))
object MockServer extends App {

  private implicit val system = ActorSystem("MockServer")
  private implicit val materializer = ActorMaterializer()

  private def routes: Route =
    pathEndOrSingleSlash {
      complete("Server up and running")
    } ~
      pathPrefix("health") {
        pathEndOrSingleSlash {
          get {
            complete(OK)
          }
        }
      }

  Http().bindAndHandle(routes, "localhost", 8080)
}
