package webserver


import akka.Done
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep}
import config.factory.action.JsonActionFactory
import config.factory.property.{JsonPropertyFactory, MixedReplaceVideoPropertyFactory}
import imgproc.Flows.{broadcast2TransformAndMerge, frameToBufferedImageImageFlow, frameToIplImageFlow, iplImageToFrameImageFlow}
import model._
import org.bytedeco.opencv.opencv_core.IplImage
import sources.FrameSource
import spray.json.JsObject
import webserver.json.JsonModel._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object RouteGenerator {
  val receivedPostMessage = "{\"message\": \"Action request received\"}"

  def completeWithErrorMessage(throwable: Throwable):StandardRoute = complete(HttpResponse(500, entity =
    HttpEntity(ContentTypes.`application/json`, "{\"error\":\"" + throwable.getMessage + "\"}")))

  def generateGet(completePath: PathMatcher[Unit], value: () => String): Route = {
    (path(completePath) & get) {
      complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, value())))
    }
  }

  def generateGet2(completePath: PathMatcher[Unit], property: Property): Route = {
    (path(completePath) & get & extractRequestContext) { ctx =>
      implicit val exe: ExecutionContextExecutor = ctx.executionContext
      property.source match {
        case Success(stream) => complete(HttpResponse(200, entity = HttpEntity(property.contentType, stream)))
        case Failure(exception) => completeWithErrorMessage(exception)
      }
    }
  }

  //TODO: Test these actions
  def generatePost(completePath: PathMatcher[Unit], action: Action): Route = {
    (path(completePath) & post & extractRequest & extractRequestContext) { (req, ctx) =>
      implicit val mat: Materializer = ctx.materializer
      implicit val exe: ExecutionContextExecutor = ctx.executionContext
      val actionExecution = req.entity.dataBytes.toMat(action.sink)(Keep.right).run()
      onComplete(actionExecution) {
        case Failure(exception) => completeWithErrorMessage(exception)
        case Success(Failure(exception)) => completeWithErrorMessage(exception)
        case Success(Success(Done))  => complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, receivedPostMessage)))
      }


      /* req.entity.contentType != action.contentType =>
        complete(HttpResponse(415))*/
      //TODO: check if action.contentType match the post content type
    }
  }

  def generateListRoute(jsObject: JsObject, fullPath: PathMatcher[Unit]): Route = {
    generateGet(fullPath, () => jsObject.compactPrint)
  }

  def generatePropertyListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    generateListRoute(properties(dt), startingPath / "properties")
  }

  def generateActionListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    generateListRoute(actions(dt), startingPath / "actions")
  }

  def generatePropertiesRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat(
      dt.properties.map(p => generateGet2(startingPath / "properties" / p.name, p)) toList :_*
    )
  }

  def generateActionsRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat (
      dt.actions.map(a => generatePost(startingPath / "actions" / a.name, a)) toList :_*
    )
  }

  def generateDigitalTwinRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generatePropertiesRoutes(dt, startingPath),
      generatePropertyListRoute(dt, startingPath),
      generateActionsRoutes(dt, startingPath),
      generateActionListRoute(dt, startingPath),
      generateGet(startingPath / "name", () => dt.name)
    )
  }

  def generateHomeRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    val path = startingPath / "home"
    concat(
      generateDigitalTwinRoutes(home, path),
      generateGet(path, () => homeFormat.write(home).compactPrint),
      generateListRoute(floors(home), path / "floors"),
      generateFloorsRoutes(home, path)
    )
  }

  def generateFloorRoutes(floor: Floor, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(floor, startingPath),
      generateGet(startingPath, () => floorFormat.write(floor).compactPrint),
      generateListRoute(rooms(Right(floor)), startingPath / "rooms"),
      generateRoomsRoutes(floor, startingPath)
    )
  }

  def generateFloorsRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    concat( home.floors.map(f => generateFloorRoutes(f, startingPath / "floors" / f.name)) toList :_* )
  }

  def generateRoomRoutes(room: Room, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(room, startingPath),
      generateGet(startingPath, () => roomFormat.write(room) compactPrint),
      generateListRoute(doors(room), startingPath / "doors"),
      generateListRoute(windows(room), startingPath / "windows"),
      generateGatewaysRoutes(room, startingPath)
    )
  }

  def generateRoomsRoutes(floor: Floor, startingPath: PathMatcher[Unit]): Route = {
    concat( floor.rooms.map(r => generateRoomRoutes(r, startingPath / "rooms" / r.name)) toList :_* )
  }

  def generateGatewayRoutes(gateway: Gateway, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(gateway, startingPath),
      generateGet(startingPath, () => gatewayFormat.write(gateway) compactPrint),
      generateListRoute(rooms(Left(gateway)), startingPath / "rooms")
    )
  }

  def generateGatewaysRoutes(room: Room, startingPath: PathMatcher[Unit]): Route = {
    concat(
      room.gateways.map {
        case d: Door => generateGatewayRoutes(d, startingPath / "doors" / d.name)
        case w: Window => generateGatewayRoutes(w, startingPath / "windows" / w.name)
      } toList: _*
    )
  }

  def generateRoutes(home: Home, startingPath: PathMatcher[Unit]): Route =
    respondWithDefaultHeader(`Access-Control-Allow-Origin`.*) {
      {
        concat(generateHomeRoutes(home, startingPath))
      }
    }
}

object Test extends App {

  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.ActorMaterializer
  import config.ConfigDsl._
  import webserver.json.JsonModel._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val external = room()
  val hallway = room()
  val bedRoom = room().withAction(
    JsonActionFactory[Int]("action", v => println(s"Acting with $v"))
  )
  //Source --> Flow --> Sink
  val h = home("home")(
    floor("first", 0)(
      hallway,
      bedRoom
    )
  )
  h.withProperties(time_now(),
    JsonPropertyFactory.dynamic[Int]("lol", () => Failure(new Exception("failed")), "nothing"))
  h.withAction(
    JsonActionFactory[Int]("action", v => println(s"Acting with $v"))
  )
  import imgproc.RichIplImage._
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
  val stream = FrameSource.video("http://192.168.1.237/video.cgi")
    .via(frameToIplImageFlow)
    .via(movDetector)
    .via(iplImageToFrameImageFlow)
    .via(frameToBufferedImageImageFlow)
  val asd = MixedReplaceVideoPropertyFactory("video", () => stream)

  door(bedRoom -> hallway)
  door(hallway -> external).withProperties(
    time_now(),
    tag("color", "green"),
    asd

    //http_object[SensorState]("garage", garageReq)
  )

  val build: Home = h.build()

  val route = RouteGenerator.generateRoutes(build, "api")

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}