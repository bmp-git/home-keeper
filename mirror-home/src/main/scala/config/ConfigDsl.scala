package config

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.Source
import config.factory.property.{HttpPropertyFactory, MqttPropertyFactory, PropertyFactory}
import config.factory.topology
import config.factory.topology._
import model.{Home, Property}
import spray.json.{JsObject, JsonFormat}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import spray.json.DefaultJsonProtocol._

object ConfigDsl {

  type BrokerAddress = String //TODO: move

  def home(name: String): HomeFactory = HomeFactory(name)

  def floor(name: String): FloorFactory = FloorFactory(name)

  def room()(implicit name: sourcecode.Name): RoomFactory = room(name.value)

  def room(name: String): RoomFactory = RoomFactory(name)

  def door(rooms: (RoomFactory, RoomFactory)): DoorFactory = rooms match {
    case (roomA, roomB) => topology.DoorFactory(roomA.name + "<->" + roomB.name, rooms)
  }

  def window(rooms: (RoomFactory, RoomFactory)): WindowFactory = rooms match {
    case (roomA, roomB) =>
      val window = topology.WindowFactory(roomA.name + "<->" + roomB.name, rooms)
      roomA.withGateways(window)
      roomB.withGateways(window)
      window
  }

  //Properties
  implicit val system: ActorSystem = ActorSystem()

  def time_now(): PropertyFactory[Long] = PropertyFactory.safe("time",
    Source.tick(0.second, 1.second, None).map(_ => System.currentTimeMillis))

  def tag(name: String, value: String): PropertyFactory[String] = PropertyFactory.static(name, value)

  def mqtt_bool(name: String, stateTopic: String, truePayload: String, falsePayload: String)
               (implicit brokerAddress: BrokerAddress): PropertyFactory[Boolean] =
    MqttPropertyFactory.payloads(name, brokerAddress, stateTopic).flatMap {
      case `truePayload` => Success(true)
      case `falsePayload` => Success(false)
      case v => Failure(new Exception(s"$name MqttBool property: $v match failure"))
    }

  def http_state(name: String, request: HttpRequest, pollingFreq: FiniteDuration): PropertyFactory[String] = ???


  def http_object[T: JsonFormat](name: String, httpRequest: HttpRequest): PropertyFactory[T] =
    HttpPropertyFactory.toObject[T](name, httpRequest, 1000.millis)

  def text_file(name:String, path:String):Property[String] = new Property[String] { //TODO: remove lol (or do it properly)
    override def name: String = name

    val s = scala.io.Source.fromFile("C:\\Users\\Edo\\Desktop\\jwt.txt")
    val v = s.mkString
    s.close()
    override def value: Try[String] = Success(v)

    override def jsonFormat: JsonFormat[String] = implicitly[JsonFormat[String]]
  }
}


object Test extends App {

  import ConfigDsl._


  case class SensorState(entity_id: String, last_changed: String, last_updated: String, state: String)

  implicit val sensorStateFormat: JsonFormat[SensorState] = lazyFormat(jsonFormat4(SensorState))

  implicit val brokerAddress: BrokerAddress = "192.168.1.10:1883"
  val hassAuth = Authorization(OAuth2BearerToken(text_file("jwt", "C:\\Users\\Edo\\Desktop\\jwt.txt").value.get))
  val garageReq = HttpRequest(uri = "https://hass.brb.dynu.net/api/states/sensor.consumo_garage").withHeaders(hassAuth)


  val external = room()
  val hallway = room()
  val bedRoom = room()

  val h = home("home")(
    floor("floor level")(
      hallway,
      bedRoom
    )
  )

  //door(bedRoom -> hallway)
  door(hallway -> external).withProperties(
    time_now(),
    tag("color", "green"),
    mqtt_bool("PC", "stat/shelly25_1/POWER1", "ON", "OFF"),
    mqtt_bool("Letto", "stat/shelly25_1/POWER2", "ON", "OFF"),
    http_object[SensorState]("garage", garageReq)
  )


  val build: Home = h.build()
  println(build)

  def printProperty[T](p: Property[T]): Unit = {
    val state = p.value match {
      case Failure(exception) => exception match {
        case _: NoSuchElementException => "Unknown"
        case ex => ex.getMessage
      }
      case Success(v) => v.toString
    }
    println(s"${p.name}: $state")
  }

  while (true) {
    build.floors.head.rooms.head.gateways.head.properties.foreach(p => {
      printProperty(p)
    })

    Thread.sleep(1000)
  }
  /*println(Eval[Unit](
    """
      |import config.ConfigDsl._
      |val hallway = room()
      |val bedRoom = room()
      |val d = door(bedRoom -> hallway)
      |val buildBedRoom = bedRoom.build()
      |val buildD = d.build()
      |println(buildBedRoom)
      |println(buildD)
      |println(buildBedRoom.gateways)
      |println(buildD.rooms)
      |""".stripMargin))*/
}