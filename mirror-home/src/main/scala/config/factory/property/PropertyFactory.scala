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
  def map2[B: JsonFormat](f: Try[T] => Try[B]): PropertyFactory[B] = new PropertyFactory[B] {
    override def name: String = PropertyFactory.this.name

    override protected def oneTimeBuild(): Property[B] = PropertyFactory.this.build().map2(f)
  }

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
  def static[T: JsonFormat](propertyName: String, staticValue: T): PropertyFactory[T] = dynamic(propertyName, () => staticValue)

  def dynamic[T: JsonFormat](propertyName: String, dynamicValue: () => T): PropertyFactory[T] = new PropertyFactory[T] {
    override def name: String = propertyName

    override protected def oneTimeBuild(): Property[T] = new Property[T] {
      override def name: String = propertyName

      override def value: Try[T] = Success(dynamicValue())

      override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
    }
  }

  def safe[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[T, _])(implicit system: ActorSystem): PropertyFactory[T]
  = apply(propertyName, () => outputStreamFactory().map(Success.apply))

  def apply[T: JsonFormat](propertyName: String, outputStreamFactory: () => Source[Try[T], _])(implicit system: ActorSystem): PropertyFactory[T]
  = new ActivePropertyFactory[T] {
    override def actorSystem: ActorSystem = system

    override def name: String = propertyName

    override def output: Source[Try[T], _] = outputStreamFactory()

    override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
  }
}
