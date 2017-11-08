package si10582

import scalaz._, Scalaz._

object Repro {
  val valids: List[ValidationNel[Int, String]] = List(1.failureNel[String], "hello".successNel[Int])
  // val valids: List[Validation[NonEmptyList[Int], String]] = List(1.failureNel[String], "hello".successNel[Int])
  valids.separate
}
