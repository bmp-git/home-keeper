package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.factory.OneTimeFactory
import model.{JsonProperty, Property}
import spray.json.{JsObject, JsonFormat}
import utils.RichProperty._

import scala.util.{Success, Try}
trait PropertyFactory[T] extends OneTimeFactory[Property[T]] {
  def name: String
}
trait JsonPropertyFactory[T] extends PropertyFactory[T] {
  def name: String

  //TODO: remove if unused
  /*def map2[B: JsonFormat](f: Try[T] => Try[B]): JsonPropertyFactory[B] = new JsonPropertyFactory[B] {
    override def name: String = JsonPropertyFactory.this.name

    override protected def oneTimeBuild(): JsonProperty[B] = JsonPropertyFactory.this.build().map2(f)
  }

  def map[B: JsonFormat](f: T => B): JsonPropertyFactory[B] = new JsonPropertyFactory[B] {
    override def name: String = JsonPropertyFactory.this.name

    override protected def oneTimeBuild(): JsonProperty[B] = JsonPropertyFactory.this.build().map(f)
  }

  def flatMap[B: JsonFormat](f: T => Try[B]): JsonPropertyFactory[B] = new JsonPropertyFactory[B] {
    override def name: String = JsonPropertyFactory.this.name

    override protected def oneTimeBuild(): JsonProperty[B] = JsonPropertyFactory.this.build().flatMap(f)
  }

  def recoverWith(f: Throwable => T): JsonPropertyFactory[T] = new JsonPropertyFactory[T] {
    override def name: String = JsonPropertyFactory.this.name

    override protected def oneTimeBuild(): JsonProperty[T] = JsonPropertyFactory.this.build().recoverWith(f)
  }*/
}



object JsonPropertyFactory {
  def static[T: JsonFormat](propertyName: String, staticValue: T): JsonPropertyFactory[T] = dynamic(propertyName, () => staticValue)

  def dynamic[T: JsonFormat](propertyName: String, dynamicValue: () => T): JsonPropertyFactory[T] = new JsonPropertyFactory[T] {
    override def name: String = propertyName

    override protected def oneTimeBuild(): JsonProperty[T] = new JsonProperty[T] {
      override def name: String = propertyName

      override def value: Try[T] = Success(dynamicValue())

      override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
    }
  }

  def safe[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[T, _])(implicit system: ActorSystem): JsonActivePropertyFactory[T]
  = apply(propertyName, () => outputStreamFactory().map(Success.apply))

  def apply[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[Try[T], _])(implicit system: ActorSystem): JsonActivePropertyFactory[T]
  = new JsonActivePropertyFactory[T] {
    override def actorSystem: ActorSystem = system

    override def name: String = propertyName

    override def output: Source[Try[T], _] = outputStreamFactory()

    override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
  }
}
