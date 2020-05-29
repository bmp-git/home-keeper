package utils

import akka.stream.scaladsl.Source

import scala.util.{Failure, Success, Try}

object RichTrySource {

  implicit class RichTrySource[T, M](source: Source[Try[T], M]) {

    def mapValue[B](f: T => B): Source[Try[B], M] = source.map({
      case Success(value) => Success(f(value))
      case Failure(exception) => Failure(exception)
    })

    def filterValue(f: T => Boolean): Source[Try[T], M] = source.filter({
      case Success(value) => f(value)
      case Failure(_) => true
    })

    def tryMapValue[B](f: T => Try[B]): Source[Try[B], M] = source.map({
      case Success(value) => f(value)
      case Failure(ex) => Failure(ex)
    })

    def recoverWith(f: Throwable => T): Source[Try[T], M] = source.map({
      case Failure(ex) => Success(f(ex))
      case success => success
    })

    def collectValue[B](f: PartialFunction[T, B]): Source[Try[B], M] =
      source.filterValue(v => f.isDefinedAt(v)).mapValue(f)

    def scanValue[B](zero: B)(f: (B, T) => B): Source[Try[B], M] = {
      var r = zero
      source.mapValue(v => {
        r = f(r, v)
        r
      })
    }

    def ignoreFailures: Source[Try[T], M] = source.filter(_.isSuccess)
  }
}
