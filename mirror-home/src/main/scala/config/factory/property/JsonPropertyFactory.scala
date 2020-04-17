package config.factory.property

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import spray.json.JsonFormat

import scala.util.{Success, Try}

trait JsonPropertyFactory[T] extends PropertyFactory[T]

object JsonPropertyFactory {
  def static[T: JsonFormat](propertyName: String, staticValue: T): JsonPropertyFactory[T] =
    safeDynamic(propertyName, () => staticValue)

  def dynamic[T: JsonFormat](propertyName: String, dynamicValue: () => Try[T]): JsonPropertyFactory[T] =
    new JsonValuePropertyFactory[T](propertyName, dynamicValue)

  def safeDynamic[T: JsonFormat](propertyName: String, dynamicValue: () => T): JsonPropertyFactory[T] =
    dynamic(propertyName, () => Success(dynamicValue()))

  def fromStream[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[Try[T], _])
                               (implicit system: ActorSystem): JsonStreamPropertyFactory[T] =
    new JsonStreamPropertyFactory[T](propertyName, outputStreamFactory)
}