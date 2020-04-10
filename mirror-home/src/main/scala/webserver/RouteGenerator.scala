package webserver

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import config.factory.action.ActionFactory
import model.{Action, DigitalTwin, Door, Floor, Gateway, Home, Room, Window}
import spray.json.{JsObject, JsonParser, ParserInput}

import scala.io.StdIn
import webserver.json.JsonModel._

import scala.util.{Failure, Success, Try}

object RouteGenerator {
  val receivedPostMessage = "Action request received"

  def generateGet(completePath: PathMatcher[Unit], value: () => String): Route = {
    (path(completePath) & get) {
      complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, value())))
    }
  }

  //TODO: Test these actions
  def generatePost(completePath: PathMatcher[Unit], action: Action[_]): Route = {
    //TODO: maybe something like case class Value[T](value: T)
    (path(completePath) & post & entity(as[String])) { raw =>
      //TODO: Add a tryTrig to actions to understand if the payload is incorrect?
      Try { action.trigFromJson(JsonParser(ParserInput(raw))) } match {
        case Success(_) => complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, receivedPostMessage)))
        case Failure(_) => complete(HttpResponse(500))
      }
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
      dt.properties.map(p => generateGet(startingPath / "properties" / p.name, () => property(p).compactPrint)) toList :_*
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
      } toList :_*
    )
  }

  def generateRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    concat( generateHomeRoutes(home, startingPath) )
  }
}

object Test extends App {
  import config.ConfigDsl._
  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.HttpRequest
  import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
  import spray.json.JsonFormat
  import webserver.json.JsonModel._

  import akka.stream.ActorMaterializer

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val external = room()
  val hallway = room()
  val bedRoom = room().withAction(
    ActionFactory[Int]("action", v => println(s"Acting with $v"))
  )

  val h = home("home")(
    floor("first")(
      hallway,
      bedRoom
    )
  )
  h.withProperties(time_now())

  door(bedRoom -> hallway)
  door(hallway -> external).withProperties(
    time_now(),
    tag("color", "green"),
    //http_object[SensorState]("garage", garageReq)
  )

  val build: Home = h.build()

  val route = RouteGenerator.generateRoutes(build, "api")

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}