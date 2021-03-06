package config.factory.property

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import model.JsonProperty
import spray.json.JsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class JsonStreamPropertyFactory[T: JsonFormat](override val name: String, output: () => Source[Try[T], _], semantic: String, initial: Option[T])(implicit actorSystem: ActorSystem)
  extends JsonPropertyFactory[T] {

  override def oneTimeBuild(): JsonProperty[T] = new JsonProperty[T] {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    private var v: Try[T] = initial match {
      case Some(value) => Success(value)
      case None => Failure(new NoSuchElementException("No value yet."))
    }
    val end: Future[Done] = output().runForeach(x => v = x)
    end.onComplete { _ => //Probably an anomaly
      println(s"JsonStreamProperty '$name' terminated.")
    }

    override def name: String = JsonStreamPropertyFactory.this.name

    override def value: Try[T] = v

    override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]

    override def semantic: String = JsonStreamPropertyFactory.this.semantic
  }
}
