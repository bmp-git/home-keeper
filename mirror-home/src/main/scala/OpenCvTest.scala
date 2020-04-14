import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaType}
import akka.http.scaladsl.server.Directives.{complete, path, respondWithHeaders, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import imgproc.Flows._
import imgproc.RichIplImage._
import org.bytedeco.opencv.opencv_core._
import sources.{FrameSource, RealTimeSourceMulticaster}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn

object OpenCvTest extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val filename = "http://192.168.1.237/video.cgi"

  val tIdentity = Flow[IplImage]

  val backgroundFlow = Flow[IplImage].scan[Option[IplImage]](None)({
    case (Some(lastResult), image) => Some(lastResult.merge(image, 0.03))
    case (None, image) => Some(image)
  }).collect {
    case Some(image) => image
  }
  val backGroundDiffFlow = broadcast2TransformAndMerge(backgroundFlow, tIdentity,
    (background: IplImage, source: IplImage) => background absDiff source).map(_.threshold(80))
  val movDetector = broadcast2TransformAndMerge(backGroundDiffFlow, tIdentity,
    (diff: IplImage, source: IplImage) => diff.rectangles(source))
  val stream = FrameSource.video(filename)
    .via(frameToIplImageFlow)
    .via(movDetector)
    .via(iplImageToFrameImageFlow)
    .via(frameToBufferedImageImageFlow)
    .via(mimeFrameEncoderFlow)


  val ct = ContentType(MediaType.customMultipart("x-mixed-replace", Map("boundary" -> "--boundary")))
  val multicaster = RealTimeSourceMulticaster[ByteString](() => stream.idleTimeout(1.seconds), null, 50)
  val route =
    path("hello") {
      if (multicaster.isTerminated) {
        multicaster.retry()
        get {
          complete(HttpResponse(500))
        }
      } else {
        respondWithHeaders(RawHeader("Cache-Control", "no-cache"), RawHeader("Pragma", "no-cache")) {
          complete(HttpEntity(ct, multicaster.cast))
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
