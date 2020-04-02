package config.factory.property

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import config.factory.OneTimeFactory
import model.Property
import utils.RichProperty._

import scala.util.{Success, Try}

trait PropertyFactory[T] extends OneTimeFactory[Property[T]] {
  def map[B](f: T => B): PropertyFactory[B] = () => build().map(f) //TODO: remove

  def flatMap[B](f: T => Try[B]): PropertyFactory[B] = () => build().flatMap(f)

  def recoverWith(f: Throwable => T): PropertyFactory[T] = () => build().recoverWith(f)
}



object PropertyFactory {
  def static[T](propertyName: String, staticValue: T): PropertyFactory[T] = () => new Property[T] {
    override def name: String = propertyName

    override def value: Try[T] = Success(staticValue)
  }

  def safe[T](propertyName: String, outputStream: Source[T, _])(implicit system: ActorSystem): PropertyFactory[T]
  = apply(propertyName, outputStream.map(Success.apply))

  def apply[T](propertyName: String, outputStream: Source[Try[T], _])(implicit system: ActorSystem): PropertyFactory[T]
  = new ActivePropertyFactory[T] {
    override def actorSystem: ActorSystem = system

    override def name: String = propertyName

    override def output: Source[Try[T], _] = outputStream
  }
}
