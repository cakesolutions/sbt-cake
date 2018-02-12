// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalatest._

class ValueDiscardFailure extends FreeSpec {
  "Testing with future int value discarded" in {
    val ignore: Unit = {
      Future(42)
    }

    assert(ignore.isInstanceOf[Unit])
  }
}
