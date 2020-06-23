package sources

import java.security.cert.X509Certificate

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}
import spray.json.{JsonParser, JsonReader, ParserInput}
import utils.RichFuture._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object HttpSource {
  val responseDownloadTimeout: FiniteDuration = 1.second
  val responseMaxBufferSize: Long = 5 * 1024 * 1024
  val trustfulSslContext: SSLContext = { //TODO: not safe, add the possibility to check from user
    object NoCheckX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

      override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

      override def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
    }
    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), null)
    context
  }

  def responses(request: HttpRequest, pollingFreq: FiniteDuration)
               (implicit actorSystem: ActorSystem): Source[Try[HttpResponse], Cancellable] = {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    val http = Http()
    Source.tick(0.second, pollingFreq, NotUsed)
      .map(_ => http.singleRequest(request, ConnectionContext.https(trustfulSslContext)).toTry)
      .mapAsync(1)(identity)
  }

  def bodyStream(request: HttpRequest)(implicit actorSystem: ActorSystem): Source[ByteString, Any] = {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val http = Http()
    val f = http.singleRequest(request, ConnectionContext.https(trustfulSslContext)).map[Source[ByteString, Any]] {
      case value if value.status.isSuccess =>
        value.headers.foreach(h => println(s"${h.name()}: ${h.value()}"))
        println(value.protocol.value)
        value.entity.withoutSizeLimit().dataBytes
      case value => throw new Exception(s"Http request failed with code ${value.status.intValue()}: ${value.status.reason()}")
    }
    Source.fromFuture(f).flatMapConcat(identity)
  }

  def bodies(request: HttpRequest, pollingFreq: FiniteDuration)(implicit actorSystem: ActorSystem): Source[Try[String], Cancellable] =
    responses(request, pollingFreq).via(bodyExtractor)

  def objects[T: JsonReader](request: HttpRequest, pollingFreq: FiniteDuration)(implicit actorSystem: ActorSystem): Source[Try[T], Cancellable] =
    responses(request, pollingFreq).via(bodyExtractor).via(objectExtractor[T])

  private def objectExtractor[T: JsonReader]: Flow[Try[String], Try[T], NotUsed] =
    Flow[Try[String]].map(_.map(body => Try(JsonParser(ParserInput(body)).convertTo[T])).flatten)

  private def bodyExtractor(implicit actorSystem: ActorSystem): Flow[Try[HttpResponse], Try[String], NotUsed] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    Flow[Try[HttpResponse]]
      //Keep only response with code 2xx
      .map[Try[HttpResponse]]({
        case Success(response) => response.status match {
          case sc if sc.isSuccess() => Success(response)
          case sc => Failure(new Exception(s"Http request failed with code ${sc.intValue()}: ${sc.reason()}"))
        }
        case failure => failure
      })
      //Load and convert body response to string
      .map[Future[Try[String]]] {
        case Success(value) => value.entity
          .toStrict(responseDownloadTimeout, responseMaxBufferSize)
          .map(_.data.utf8String).toTry
        case Failure(ex) => Future[Try[String]](Failure(ex))
      }
      .mapAsync[Try[String]](1)(v => v)
  }
}
