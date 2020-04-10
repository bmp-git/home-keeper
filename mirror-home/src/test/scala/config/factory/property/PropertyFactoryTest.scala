package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalatest.FunSuite
import sources.HttpSource
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class PropertyFactoryTest extends FunSuite {
  test("Static property") {
    val p = PropertyFactory.static("name", 123.4).build()
    assert(p.name == "name")
    assert(p.value.getOrElse(0) == 123.4)
  }

  test("Http property") {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    case class ApiTime(currentFileTime: Long)
    implicit val formatter: RootJsonFormat[ApiTime] = jsonFormat1(ApiTime)

    //TODO: unsafe
    val source = HttpSource.objects[ApiTime](HttpRequest(uri = "http://worldclockapi.com/api/json/est/now"), 500.millis)

    val taken = 5
    val seq: Future[Seq[Try[ApiTime]]] = source.take(taken).runWith(Sink.seq[Try[ApiTime]])
    seq.onComplete({
        case Failure(_) => fail()
        case Success(value) =>
          assert(value.size == taken)
          assert(value.count(_.isSuccess) == taken)
          assert(value.filter(_.isSuccess).map(_.get).distinct.size == taken)
          seq.synchronized {
            seq.notify()
          }
      })
    seq.synchronized {
      seq.wait()
    }
  }
}
