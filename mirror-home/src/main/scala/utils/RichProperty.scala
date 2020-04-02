package utils

import model.Property
import spray.json.JsonFormat

import scala.util.{Failure, Success, Try}

object RichProperty {
  implicit class RickProperty[T](property: Property[T]) {
    def map[B:JsonFormat](f: T => B): Property[B] = new Property[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value.map(f)

      override def jsonFormat: JsonFormat[B] = implicitly[JsonFormat[B]]
    }

    def flatMap[B:JsonFormat](f: T => Try[B]): Property[B]  = new Property[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value match {
        case Failure(exception) => Failure[B](exception)
        case Success(value) => f(value)
      }

      override def jsonFormat: JsonFormat[B] = implicitly[JsonFormat[B]]
    }

    def recoverWith(f: Throwable => T):Property[T] = new Property[T] {
      override def name: String = property.name

      override def value: Try[T] = property.value match {
        case Failure(exception) => Success(f(exception))
        case success => success
      }

      override def jsonFormat: JsonFormat[T] = property.jsonFormat
    }
  }
}
