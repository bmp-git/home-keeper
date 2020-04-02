package utils

import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object RichFuture {

  implicit class RichFuture[T](future: Future[T]) {
    def toTry(implicit executionContext: ExecutionContextExecutor): Future[Try[T]] =
      future.map(Success.apply)
        .recoverWith({
          case ex => Future[Try[T]](Failure(ex))
        })
  }
}
