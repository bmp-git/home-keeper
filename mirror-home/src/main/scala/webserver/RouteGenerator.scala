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

  def generatePropertyGet(completePath: PathMatcher[Unit], property: Property): Route = {
    (path(completePath) & get & extractRequestContext) { ctx =>
      implicit val exe: ExecutionContextExecutor = ctx.executionContext
      property.source match {
        case Success(stream) => complete(HttpResponse(200, entity = HttpEntity(property.contentType, stream)))
        case Failure(exception) => completeWithErrorMessage(exception)
      }
    }
  }

  //TODO: Test these actions
  def generateActionPost(completePath: PathMatcher[Unit], action: Action): Route = {
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
      dt.properties.map(p => generatePropertyGet(startingPath / "properties" / p.name, p)) toList :_*
    )
  }

  def generateActionsRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat (
      dt.actions.map(a => generateActionPost(startingPath / "actions" / a.name, a)).toList ++
        dt.actions.map(a => generateGet(startingPath / "actions" / a.name, () => a.jsonDescription.compactPrint)) :_*
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