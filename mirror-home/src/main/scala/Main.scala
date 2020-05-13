import akka.stream.scaladsl.Flow
import config.factory.property.{JsonPropertyFactory, MixedReplaceVideoPropertyFactory}
import imgproc.Flows.{broadcast2TransformAndMerge, frameToBufferedImageImageFlow, frameToIplImageFlow, iplImageToFrameImageFlow}
import model.Home
import org.bytedeco.opencv.opencv_core.IplImage
import sources.FrameSource
import webserver.RouteGenerator

import scala.io.StdIn
import scala.util.Failure

object Main extends App {

  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.ActorMaterializer
  import config.ConfigDsl._
  import imgproc.RichIplImage._
  import webserver.json.JsonModel._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  val mario = user("mario")
  val luigi = user("luigi")

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
  val localStream = FrameSource.video("http://192.168.1.237/video.cgi")
    .via(frameToIplImageFlow)
    .via(movDetector)
    .via(iplImageToFrameImageFlow)
    .via(frameToBufferedImageImageFlow)
  val localVideo = MixedReplaceVideoPropertyFactory("video", () => localStream)

  val remoteStream = FrameSource.video("http://185.39.101.26/mjpg/video.mjpg")
    .via(frameToIplImageFlow)
    .via(movDetector)
    .via(iplImageToFrameImageFlow)
    .via(frameToBufferedImageImageFlow)
  val remoteVideo = MixedReplaceVideoPropertyFactory("video", () => remoteStream)

  val external = room().withProperties(remoteVideo)
  val cucina = room()
  val cameraDaLetto = room().withProperties(localVideo)
  val corridoio = room()
  val bagnoRosa = room()
  val bagnoVerde = room().withProperties(JsonPropertyFactory.dynamic[Int]("FailedProp", () => Failure(new Exception("failed")), "nothing"))
  val cameraMia = room()
  val ripostiglio = room()
  val sala = room()

  val disimpegno = room()
  val bagnoMarrone = room()

  val myHome = home("home") (
    floor("firstfloor", 0).withProperties(time_now(), tag("Tag", 10)) (
      cucina,
      cameraDaLetto,
      corridoio,
      bagnoRosa,
      bagnoVerde,
      cameraMia,
      ripostiglio,
      sala,
      external
    ),
    floor("secondfloor", 1)(
      disimpegno,
      bagnoMarrone
    ),
    floor("basement", -1)(
    )
      .withAction(trig("trigAction"))
      .withAction(turn("turnAction"))
  )
    .withAction(turn("siren"))
    .withUsers(mario, luigi)


  door(sala -> external)
  door(sala -> corridoio)
  door(sala -> cucina)
  door(ripostiglio -> corridoio)
  door(bagnoVerde -> corridoio)
  door(bagnoRosa -> corridoio)
  door(cameraDaLetto -> corridoio)
  door(cameraMia -> corridoio)
  door(ripostiglio -> external)
  door(cameraMia -> external)
  door(cucina -> external)
  door(cameraDaLetto -> external)

  window(sala -> external)
  window(bagnoVerde -> external)
  window(bagnoRosa -> external)

  door(disimpegno -> bagnoMarrone)

  cucina.withProperties(time_now(), tag("lol", 20))
  myHome.withProperties(time_now())

  val build: Home = myHome.build()

  val route = RouteGenerator.generateRoutes(build, "api")

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
