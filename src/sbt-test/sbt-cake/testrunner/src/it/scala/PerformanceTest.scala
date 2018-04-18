// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import io.gatling.core.Predef._
import io.gatling.http.HeaderNames
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.language.postfixOps

class PerformanceTest extends Simulation {

  val hostname = sys.env.getOrElse("CI_HOST", "localhost")
  val httpConf = http.baseURL(s"http://$hostname:8080")

  val readClients = scenario("Clients").exec(HealthCheck.refreshManyTimes)

  setUp(
    readClients.inject(rampUsers(1) over (1 seconds)).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gt(95)
  )
}

object HealthCheck {

  def refreshAfterOneSecond = {
    exec(
      http("Health")
        .get("/health")
        .header(HeaderNames.Host, "localhost")
        .check(status.is(200))
    ).pause(1)
  }

  val refreshManyTimes = repeat(1) {
    refreshAfterOneSecond
  }
}
