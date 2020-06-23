package webserver


import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException

import akka.Done
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.server.{PathMatcher, Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import model._
import spray.json.{JsValue, JsonWriter}
import webserver.json.JsonModel._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object RouteGenerator {
  val receivedPostMessage = "{\"message\":\"Action request received\"}"

  def completeWithErrorMessage(code: Int, message: String): StandardRoute =
    complete(HttpResponse(code, entity = HttpEntity(ContentTypes.`application/json`, "{\"error\":\"" + message + "\"}")))

  def completeWithErrorMessage(throwable: Throwable): StandardRoute = {
    val (code, message) = throwable match {
      case _: NoSuchFileException | _: FileNotFoundException => (404, "File not found.")
      case _: NoSuchElementException => (404, "No such element yet.")
      case _ => (500, "Unknown error: " + throwable.getMessage)
    }
    completeWithErrorMessage(code, message)
  }

  def generateJsonGet[T: JsonWriter](completePath: PathMatcher[Unit], value: T)(implicit auth: AuthenticationDirective[String]): Route =
    generateJsonGet(completePath, implicitly[JsonWriter[T]].write(value))

  def generateJsonGet(completePath: PathMatcher[Unit], jsValue: JsValue)(implicit auth: AuthenticationDirective[String]): Route =
    (path(completePath) & auth & get) { username =>
      complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, jsValue.compactPrint)))
    }

  def generateRawPropertyGet(completePath: PathMatcher[Unit], property: Property)(implicit auth: AuthenticationDirective[String]): Route = {
    (path(completePath) & auth & get & extractRequestContext) { (username, ctx) =>
      implicit val exe: ExecutionContextExecutor = ctx.executionContext
      property.source match {
        case Success(stream) => complete(HttpResponse(200, entity = HttpEntity(property.contentType, stream)))
        case Failure(exception) => completeWithErrorMessage(exception)
      }
    }
  }

  def generateActionPost(completePath: PathMatcher[Unit], action: Action)(implicit auth: AuthenticationDirective[String]): Route = {
    (path(completePath) & auth & post & extractRequest & extractRequestContext) { (username, req, ctx) =>
      implicit val mat: Materializer = ctx.materializer
      implicit val exe: ExecutionContextExecutor = ctx.executionContext
      //TODO: application/x-www-form-urlencoded is sent while uploading svg from home-viewer (fix from home-viewer)
      if (false && req.entity.contentType != action.contentType) {
        completeWithErrorMessage(415, "Invalid content type: the required type is '" + action.contentType + "' but '" + req.entity.contentType + "' was received")
      } else {
        val actionExecution = req.entity.dataBytes.toMat(action.sink)(Keep.right).run()
        val reqContentType = req.entity.contentType
        onComplete(actionExecution) {
          case Failure(exception) =>
            completeWithErrorMessage(exception)
          case Success(Failure(exception)) =>
            completeWithErrorMessage(exception)
          case Success(Success(Done)) =>
            complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, receivedPostMessage)))
        }
      }
    }
  }


  def generatePropertyListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    generateJsonGet(startingPath / "properties", propertiesJsArray(dt))
  }

  def generateActionListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    generateJsonGet(startingPath / "actions", actionsJsArray(dt))
  }

  def generatePropertiesRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      dt.properties.map(p => generateJsonGet(startingPath / "properties" / p.name / "name", p.name)).toList ++
        dt.properties.map(p => generateJsonGet(startingPath / "properties" / p.name, p.jsonDescription)).toList ++
        dt.properties.map(p => generateRawPropertyGet(startingPath / "properties" / p.name / "raw", p)).toList: _*
    )
  }

  def generateActionsRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      dt.actions.map(a => generateActionPost(startingPath / "actions" / a.name, a)).toList ++
        dt.actions.map(a => generateJsonGet(startingPath / "actions" / a.name, a.jsonDescription)): _*
    )
  }

  def generateDigitalTwinRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      generatePropertiesRoutes(dt, startingPath),
      generatePropertyListRoute(dt, startingPath),
      generateActionsRoutes(dt, startingPath),
      generateActionListRoute(dt, startingPath),
      generateJsonGet(startingPath / "name", dt.name)
    )
  }


  def generateUserListRoute(home: Home, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route =
    generateJsonGet(startingPath / "users", usersJsArray(home))

  def generateUsersRoutes(home: Home, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route =
    concat(home.users.map(p => generateUserRoutes(p, startingPath)).toList: _*)

  def generateUserRoutes(user: User, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = concat(
    generateJsonGet(startingPath / "users" / user.name, user),
    generateDigitalTwinRoutes(user, startingPath / "users" / user.name)
  )

  def generateHomeRoutes(home: Home, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
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

  def generateFloorRoutes(floor: Floor, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      generateDigitalTwinRoutes(floor, startingPath),
      generateJsonGet(startingPath, floor),
      generateJsonGet(startingPath / "rooms", roomsJsArray(Right(floor))),
      generateRoomsRoutes(floor, startingPath),
      generateJsonGet(startingPath / "level", floor.level)
    )
  }

  def generateFloorsRoutes(home: Home, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(home.floors.map(f => generateFloorRoutes(f, startingPath / "floors" / f.name)) toList: _*)
  }

  def generateRoomRoutes(room: Room, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      generateDigitalTwinRoutes(room, startingPath),
      generateJsonGet(startingPath, room),
      generateJsonGet(startingPath / "doors", doorsJsArray(room)),
      generateJsonGet(startingPath / "windows", windowsJsArray(room)),
      generateGatewaysRoutes(room, startingPath)
    )
  }

  def generateRoomsRoutes(floor: Floor, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(floor.rooms.map(r => generateRoomRoutes(r, startingPath / "rooms" / r.name)) toList: _*)
  }

  def generateGatewayRoutes(gateway: Gateway, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      generateDigitalTwinRoutes(gateway, startingPath),
      generateJsonGet(startingPath, gateway),
      generateJsonGet(startingPath / "rooms", roomsJsArray(Left(gateway)))
    )
  }

  def generateGatewaysRoutes(room: Room, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route = {
    concat(
      room.gateways.map {
        case d: Door => generateGatewayRoutes(d, startingPath / "doors" / d.name)
        case w: Window => generateGatewayRoutes(w, startingPath / "windows" / w.name)
      } toList: _*
    )
  }

  def generateRoutes(home: Home, startingPath: PathMatcher[Unit])(implicit auth: AuthenticationDirective[String]): Route =
    respondWithDefaultHeader(`Access-Control-Allow-Origin`.*) {
      concat(generateHomeRoutes(home, startingPath))
    }
}