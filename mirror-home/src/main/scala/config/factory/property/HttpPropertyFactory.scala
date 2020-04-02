package config.factory.property

import java.security.cert.X509Certificate

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}
import spray.json.{JsonFormat, JsonParser, JsonReader, ParserInput}
import utils.RichFuture._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import spray.json.DefaultJsonProtocol._

object HttpPropertyFactory {

  object Flows {
    val responseDownloadTimeout: FiniteDuration = 1.second
    val responseMaxBufferSize: Long = 5 * 1024 * 1024

    def responses(request: HttpRequest, pollingFreq: FiniteDuration)
                 (implicit actorSystem: ActorSystem): Source[Try[HttpResponse], Cancellable] = {
      implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
      val trustfulSslContext: SSLContext = { //TODO: not safe
        object NoCheckX509TrustManager extends X509TrustManager {
          override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

          override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

          override def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), null)
        context
      }
      val http = Http()
      Source.tick(0.second, pollingFreq, NotUsed)
        .map(_ => http.singleRequest(request, ConnectionContext.https(trustfulSslContext)).toTry)
        .mapAsync(1)(identity)
    }


    def bodyExtractor(implicit actorSystem: ActorSystem): Flow[Try[HttpResponse], Try[String], NotUsed] = {
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

    def objectSource[T: JsonReader]: Flow[Try[String], Try[T], NotUsed] =
      Flow[Try[String]].map(_.map(body => Try(JsonParser(ParserInput(body)).convertTo[T])).flatten)

    def bodySource(request: HttpRequest, pollingFreq: FiniteDuration)(implicit actorSystem: ActorSystem): Source[Try[String], Cancellable] =
      responses(request, pollingFreq).via(bodyExtractor)

    def objectSource[T: JsonReader](request: HttpRequest, pollingFreq: FiniteDuration)(implicit actorSystem: ActorSystem): Source[Try[T], Cancellable] =
      responses(request, pollingFreq).via(bodyExtractor).via(objectSource[T])
  }

  import Flows._

  def apply(name: String, request: HttpRequest, pollingFreq: FiniteDuration)
           (implicit actorSystem: ActorSystem): PropertyFactory[String] =
    PropertyFactory(name, bodySource(request, pollingFreq))

  def toObject[T: JsonFormat](name: String, request: HttpRequest, pollingFreq: FiniteDuration)
                             (implicit actorSystem: ActorSystem): PropertyFactory[T] =
    PropertyFactory(name, objectSource[T](request, pollingFreq))
}

