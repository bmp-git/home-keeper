package webserver


import akka.Done
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import model._
import spray.json.{JsValue, JsonWriter}
import webserver.json.JsonModel._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object RouteGenerator {
  val receivedPostMessage = "{\"message\": \"Action request received\"}"

  def completeWithErrorMessage(throwable: Throwable):StandardRoute = complete(HttpResponse(500, entity =
    HttpEntity(ContentTypes.`application/json`, throwable.getMessage))) //TODO: case match throwable to better match the return code

  def generateJsonGet[T:JsonWriter](completePath: PathMatcher[Unit], value: T): Route =
    generateJsonGet(completePath, implicitly[JsonWriter[T]].write(value))

  def generateJsonGet(completePath: PathMatcher[Unit], jsValue: JsValue): Route =
    (path(completePath) & get) {
      complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, jsValue.compactPrint)))
    }

  def generateRawPropertyGet(completePath: PathMatcher[Unit], property: Property): Route = {
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


  def generatePropertyListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    generateJsonGet(startingPath / "properties", propertiesJsArray(dt))
  }

  def generateActionListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    generateJsonGet(startingPath / "actions", actionsJsArray(dt))
  }

  def generatePropertiesRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat(
      //dt.properties.map(p => generateJsonGet(startingPath / "properties" / p.name / "name", p.name)).toList ++ //TODO: add path for every known attribute
      dt.properties.map(p => generateJsonGet(startingPath / "properties" / p.name, p.jsonDescription)).toList ++
        dt.properties.map(p => generateRawPropertyGet(startingPath / "properties" / p.name / "raw", p)).toList :_*
    )
  }

  def generateActionsRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat (
      dt.actions.map(a => generateActionPost(startingPath / "actions" / a.name, a)).toList ++
        dt.actions.map(a => generateJsonGet(startingPath / "actions" / a.name, a.jsonDescription)) :_*
    )
  }

  def generateDigitalTwinRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generatePropertiesRoutes(dt, startingPath),
      generatePropertyListRoute(dt, startingPath),
      generateActionsRoutes(dt, startingPath),
      generateActionListRoute(dt, startingPath),
      generateJsonGet(startingPath / "name", dt.name)
    )
  }


  def generateUserListRoute(home: Home, startingPath: PathMatcher[Unit]): Route =
    generateJsonGet(startingPath / "users", usersJsArray(home))

  def generateUsersRoutes(home: Home, startingPath: PathMatcher[Unit]): Route =
    concat(home.users.map(p => generateUserRoutes(p, startingPath)).toList: _*)

  def generateUserRoutes(user: User, startingPath: PathMatcher[Unit]): Route = concat(
    generateJsonGet(startingPath / "users" / user.name, user),
    generateDigitalTwinRoutes(user, startingPath / "users" / user.name)
  )

  def generateHomeRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    val path = startingPath / "home"
    concat(
      generateDigitalTwinRoutes(home, path),
      generateJsonGet(path, home),
      generateJsonGet(path / "floors", floorsJsArray(home)),
      generateFloorsRoutes(home, path),
      generateUsersRoutes(home, path),
      generateUserListRoute(home, path),
    )
  }

  def generateFloorRoutes(floor: Floor, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(floor, startingPath),
      generateJsonGet(startingPath, floor),
      generateJsonGet(startingPath / "rooms", roomsJsArray(Right(floor))),
      generateRoomsRoutes(floor, startingPath),
      generateJsonGet(startingPath / "level", floor.level)
    )
  }

  def generateFloorsRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    concat( home.floors.map(f => generateFloorRoutes(f, startingPath / "floors" / f.name)) toList :_* )
  }

  def generateRoomRoutes(room: Room, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(room, startingPath),
      generateJsonGet(startingPath, room),
      generateJsonGet(startingPath / "doors", doorsJsArray(room)),
      generateJsonGet(startingPath / "windows", windowsJsArray(room)),
      generateGatewaysRoutes(room, startingPath)
    )
  }

  def generateRoomsRoutes(floor: Floor, startingPath: PathMatcher[Unit]): Route = {
    concat( floor.rooms.map(r => generateRoomRoutes(r, startingPath / "rooms" / r.name)) toList :_* )
  }

  def generateGatewayRoutes(gateway: Gateway, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(gateway, startingPath),
      generateJsonGet(startingPath, gateway),
      generateJsonGet(startingPath / "rooms", roomsJsArray(Left(gateway)))
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
        concat(generateHomeRoutes(home, startingPath))
    }
}