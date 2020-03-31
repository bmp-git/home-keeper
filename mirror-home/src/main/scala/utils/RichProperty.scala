package utils

import model.Property

import scala.util.{Failure, Success, Try}

object RichProperty {
  implicit class RickProperty[T](property: Property[T]) {
    def valueMap[B](f: T => B): Property[B] = new Property[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value.map(f)
    }

    def map[B](f: Try[T] => Try[B]): Property[B] = new Property[B] {
      override def name: String = property.name

      override def value: Try[B] = f(property.value)
    }

    def flatMap[B](f: T => Try[B]): Property[B]  = new Property[B] {
      override def name: String = property.name

      override def value: Try[B] = property.value match {
        case Failure(exception) => Failure[B](exception)
        case Success(value) => f(value)
      }
    }
  }
}
