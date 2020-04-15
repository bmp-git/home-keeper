package config.factory.property

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentType
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.factory.OneTimeFactory
import model.Property

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

trait ActivePropertyFactory[T] extends PropertyFactory[T]{
  def actorSystem: ActorSystem

  def name: String

  def output: Source[Try[T], _]

  def contentType: ContentType

  def serializer: T => Source[ByteString, Any]

  override def oneTimeBuild(): Property[T] = new Property[T] {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    private var v: Try[T] = Failure(new NoSuchElementException("No value yet."))
    val end: Future[Done] = output.runForeach(x => v = x)
    end.onComplete { _ => //TODO: check if correct
      println(s"property $name terminated.")
    }

    override def name: String = ActivePropertyFactory.this.name

    override def value: Try[T] = v

    override def contentType: ContentType = ActivePropertyFactory.this.contentType

    override def serialized: Try[Source[ByteString, Any]] = value.map(serializer)
  }
}
