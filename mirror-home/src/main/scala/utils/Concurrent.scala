package utils

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

object Concurrent {
  def runWithTimeout[T](finiteDuration: FiniteDuration)(f: => T)(implicit executor: ExecutionContext): T = {
    Await.result(Future(f), finiteDuration)
  }
}
