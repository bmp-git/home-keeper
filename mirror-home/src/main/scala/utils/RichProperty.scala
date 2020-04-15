package utils

import model.{JsonProperty, Property}
import spray.json.JsonFormat

import scala.util.{Failure, Success, Try}

object RichProperty {
  implicit class RickJsonProperty[T](property: JsonProperty[T]) {
    def map2[B:JsonFormat](f: Try[T] => Try[B]): JsonProperty[B] = new JsonProperty[B] {
      override def name: String = property.name

      override def value: Try[B] = f(property.value)

      override def jsonFormat: JsonFormat[B] = implicitly[JsonFormat[B]]
    }

    def map[B:JsonFormat](f: T => B): JsonProperty[B] = new JsonProperty[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value.map(f)

      override def jsonFormat: JsonFormat[B] = implicitly[JsonFormat[B]]
    }

    def flatMap[B:JsonFormat](f: T => Try[B]): JsonProperty[B]  = new JsonProperty[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value match {
        case Failure(exception) => Failure[B](exception)
        case Success(value) => f(value)
      }

      override def jsonFormat: JsonFormat[B] = implicitly[JsonFormat[B]]
    }

    def recoverWith(f: Throwable => T):JsonProperty[T] = new JsonProperty[T] {
      override def name: String = property.name

      override def value: Try[T] = property.value match {
        case Failure(exception) => Success(f(exception))
        case success => success
      }

      override def jsonFormat: JsonFormat[T] = property.jsonFormat
    }
  }
}
