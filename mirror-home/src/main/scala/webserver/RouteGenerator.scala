package webserver

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import model.{DigitalTwin, Floor, Home}
import spray.json.{JsArray, JsObject}

import scala.io.StdIn
import webserver.json.JsonModel._

object RouteGenerator {

  def generateGet(completePath: PathMatcher[Unit], value: () => String): Route = {
    (path(completePath) & get) {
      complete(HttpResponse(200, entity = HttpEntity(ContentTypes.`application/json`, value())))
    }
  }

  def generateListRoute(jsObject: JsObject, fullPath: PathMatcher[Unit]): Route = {
    generateGet(fullPath, () => jsObject.compactPrint)
  }

  def generatePropertyListRoute[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): Route = {
    generateListRoute(properties(dt), startingPath / "properties")
  }

  def generatePropertiesRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): List[Route] = {
    dt.properties.map(p => generateGet(startingPath / "properties" / p.name, () => property(p).compactPrint)) toList
  }

  def generateDigitalTwinRoutes[T <: DigitalTwin](dt: T, startingPath: PathMatcher[Unit]): List[Route] = {
      generatePropertiesRoutes(dt, startingPath) :+
      generatePropertyListRoute(dt, startingPath) :+
      //generate post per actions
      generateGet(startingPath / "name", () => dt.name)
  }

  def generateHomeRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    val path = startingPath / "home"
    concat(
        generateDigitalTwinRoutes(home, path) :+
        generateListRoute(floors(home), path / "floors") :+
        generateGet(path, () => homeFormat.write(home).compactPrint) :+
        generateFloorsRoutes(home, path) :_*
    )
  }

  def generateFloorRoutes(floor: Floor, startingPath: PathMatcher[Unit]): Route = {
    concat(
      generateDigitalTwinRoutes(floor, startingPath) :+
      generateGet(startingPath, () => floorFormat.write(floor).compactPrint) :_*
    )
  }

  def generateFloorsRoutes(home: Home, startingPath: PathMatcher[Unit]): Route = {
    concat(home.floors.map(f => generateFloorRoutes(f, startingPath / "floors" / f.name)).toList :_*)
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
  import model.Units.BrokerAddress

  import akka.stream.ActorMaterializer

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  case class SensorState(entity_id: String, last_changed: String, last_updated: String, state: String)

  implicit val sensorStateFormat: JsonFormat[SensorState] = lazyFormat(jsonFormat4(SensorState))

  implicit val brokerAddress: BrokerAddress = "192.168.1.10:1883"
  val hassAuth = Authorization(OAuth2BearerToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJjYjA2Y2Y5OGZlOWY0YmI3Yjk3ZjEwYmQyOWY0M2E0MSIsImlhdCI6MTU3NDE3ODAwMiwiZXhwIjoxODg5NTM4MDAyfQ.QOHLiaA4-cHtV4dOXQ-nCxaHQwo2HRhd6iaJNiXvk8A"))
  val garageReq = HttpRequest(uri = "https://hass.brb.dynu.net/api/states/sensor.consumo_garage").withHeaders(hassAuth)


  val external = room()
  val hallway = room()
  val bedRoom = room()

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