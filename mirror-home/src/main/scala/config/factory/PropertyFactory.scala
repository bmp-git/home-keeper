package config.factory

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import model.Property
import utils.RichProperty._

import scala.util.{Failure, Success, Try}

trait PropertyFactory[T] extends OneTimeFactory[Property[T]] {
  def valueMap[B](f: T => B): PropertyFactory[B] = () => this.build().valueMap(f)

  def map[B](f: Try[T] => Try[B]): PropertyFactory[B] = () => this.build().map(f)

  def flatMap[B](f: T => Try[B]): PropertyFactory[B] = () => this.build().flatMap(f)
}

trait DynamicPropertyFactory[T] extends PropertyFactory[T] {
  def actorSystem: ActorSystem

  def name: String

  def output: Source[Try[T], _]

  override def oneTimeBuild(): Property[T] = new Property[T] {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    private var v: Try[T] = Failure(new NoSuchElementException("No value yet."))
    output.runForeach(x => v = x)

    override def name: String = DynamicPropertyFactory.this.name

    override def value: Try[T] = v
  }
}

object PropertyFactory {
  def static[T](propertyName: String, staticValue: T): PropertyFactory[T] = () => new Property[T] {
    override def name: String = propertyName

    override def value: Try[T] = Success(staticValue)
  }

  def safe[T](propertyName: String, outputStream: Source[T, _])(implicit system: ActorSystem): PropertyFactory[T]
  = apply(propertyName, outputStream.map(Success.apply))

  def apply[T](propertyName: String, outputStream: Source[Try[T], _])(implicit system: ActorSystem): PropertyFactory[T]
  = new DynamicPropertyFactory[T] {
    override def actorSystem: ActorSystem = system

    override def name: String = propertyName

    override def output: Source[Try[T], _] = outputStream
  }
}
