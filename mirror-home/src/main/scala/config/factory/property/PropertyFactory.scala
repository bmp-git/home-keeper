package config.factory.property

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import config.factory.OneTimeFactory
import model.Property
import spray.json.JsonFormat
import utils.RichProperty._

import scala.util.{Success, Try}

trait PropertyFactory[T] extends OneTimeFactory[Property[T]] {
  def name: String

  //TODO: remove if unused
  def map[B: JsonFormat](f: T => B): PropertyFactory[B] = new PropertyFactory[B] {
    override def name: String = PropertyFactory.this.name

    override protected def oneTimeBuild(): Property[B] = PropertyFactory.this.build().map(f)
  }

  def flatMap[B: JsonFormat](f: T => Try[B]): PropertyFactory[B] = new PropertyFactory[B] {
    override def name: String = PropertyFactory.this.name

    override protected def oneTimeBuild(): Property[B] = PropertyFactory.this.build().flatMap(f)
  }

  def recoverWith(f: Throwable => T): PropertyFactory[T] = new PropertyFactory[T] {
    override def name: String = PropertyFactory.this.name

    override protected def oneTimeBuild(): Property[T] = PropertyFactory.this.build().recoverWith(f)
  }
}



object PropertyFactory {
  def static[T: JsonFormat](propertyName: String, staticValue: T): PropertyFactory[T] = new PropertyFactory[T] {
    override def name: String = propertyName

    override protected def oneTimeBuild(): Property[T] = new Property[T] {
      override def name: String = propertyName

      override def value: Try[T] = Success(staticValue)

      override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
    }
  }

  def safe[T: JsonFormat](propertyName: String, outputStream: Source[T, _])(implicit system: ActorSystem): PropertyFactory[T]
  = apply(propertyName, outputStream.map(Success.apply))

  def apply[T: JsonFormat](propertyName: String, outputStream: Source[Try[T], _])(implicit system: ActorSystem): PropertyFactory[T]
  = new ActivePropertyFactory[T] {
    override def actorSystem: ActorSystem = system

    override def name: String = propertyName

    override def output: Source[Try[T], _] = outputStream

    override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
  }
}
