// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalatest._

class ValueDiscardSuccess extends FreeSpec with Matchers {
  "Testing with future int value discarded with magic" in {
    // Scalatest magic allows this to happen!
    Future(42)

    assert(true)
  }

  "Testing with future int value discarded" in {
    val ignore: Unit = {
      Future(42)
    }

    assert(true)
  }

  "Testing with int value discarded and flagged" in {
    val ignore: Unit = ValueDiscard[Future[Int]]{
      Future(42)
    }

    assert(true)
  }

  "Ensure multiple Scalatest matchers succeed" in {
    (2 + 3) shouldEqual 5
    Seq("Hello", "world!").mkString(" ") shouldEqual "Hello world!"
    "Hello world!".length shouldEqual 12
  }
}
