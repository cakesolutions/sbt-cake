// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{ Matchers, WordSpec }

class MockServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  "MockServer" should {
    "answer to any request to `/`" in {
      Get("/") ~> MockServer.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "Server up and running"
      }
      Post("/") ~> MockServer.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "Server up and running"
      }
    }
    "answer to GET requests to `/health`" in {
      Get("/health") ~> MockServer.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "up"
      }
    }
    "not handle a POST request to `/health`" in {
      Post("/health") ~> MockServer.routes ~> check {
        handled shouldBe false
      }
    }
    "respond with 405 when not issuing a GET to `/health` and route is sealed" in {
      Put("/health") ~> Route.seal(MockServer.routes) ~> check {
        status shouldBe StatusCodes.MethodNotAllowed
      }
    }
  }

}
