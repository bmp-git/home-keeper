package config.factory.property

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentType
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import model.{JsonProperty, Property}
import spray.json.JsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

trait JsonActivePropertyFactory[T] extends JsonPropertyFactory[T] {
  def actorSystem: ActorSystem

  def name: String

  def output: Source[Try[T], _]

  def jsonFormat: JsonFormat[T]

  override def oneTimeBuild(): JsonProperty[T] = new JsonProperty[T] {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    private var v: Try[T] = Failure(new NoSuchElementException("No value yet."))
    val end: Future[Done] = output.runForeach(x => v = x)
    end.onComplete { _ => //TODO: check if correct
      println(s"property $name terminated.")
    }

    override def name: String = JsonActivePropertyFactory.this.name

    override def value: Try[T] = v

    override def jsonFormat: JsonFormat[T] = JsonActivePropertyFactory.this.jsonFormat
  }
}
