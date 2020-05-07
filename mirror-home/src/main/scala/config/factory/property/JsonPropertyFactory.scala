package config.factory.property

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import spray.json.JsonFormat

import scala.util.{Success, Try}

trait JsonPropertyFactory[T] extends PropertyFactory

object JsonPropertyFactory {
  def static[T: JsonFormat](propertyName: String, staticValue: T, propertySemantic: String): JsonPropertyFactory[T] =
    safeDynamic(propertyName, () => staticValue, propertySemantic)

  def dynamic[T: JsonFormat](propertyName: String, dynamicValue: () => Try[T], propertySemantic: String): JsonPropertyFactory[T] =
    new JsonValuePropertyFactory[T](propertyName, dynamicValue, propertySemantic)

  def safeDynamic[T: JsonFormat](propertyName: String, dynamicValue: () => T, propertySemantic: String): JsonPropertyFactory[T] =
    dynamic(propertyName, () => Success(dynamicValue()), propertySemantic)

  def fromStream[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[Try[T], _], propertySemantic: String)
                               (implicit system: ActorSystem): JsonStreamPropertyFactory[T] =
    new JsonStreamPropertyFactory[T](propertyName, outputStreamFactory, propertySemantic)
}