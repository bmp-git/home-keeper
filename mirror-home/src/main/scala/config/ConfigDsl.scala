package config

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import config.factory.action.{ActionFactory, FileWriterActionFactory, JsonActionFactory}
import config.factory.ble.BleBeaconFactory
import config.factory.property.{FileReaderPropertyFactory, JsonPropertyFactory, PropertyFactory}
import config.factory.topology._
import model.Units.MacAddress
import model._
import model.ble.{BeaconData, RawBeaconData}
import model.mhz433.{OpenCloseData, Raw433MhzData}
import sources.{HttpSource, MqttSource}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, JsonFormat}
import utils.Lazy
import utils.RichTrySource._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ConfigDsl {

  val RESOURCE_FOLDER = "resources"

  sealed trait UserPosition
  case object Unknown extends UserPosition
  case object AtHome extends UserPosition
  case object Away extends UserPosition
  case class InRoom(floorName: String, roomName: String) extends UserPosition

  /**
   * {
   *    "type" : "unknown"
   *    "floor" : "floor" ?
   *    "room" : "room" ?
   * }
   */
  def userPositionFormat: JsonFormat[UserPosition] = {
    new JsonFormat[UserPosition] {
      override def read(json: JsValue): UserPosition = json match {
        case JsObject(fields) => fields("type") match {
          case JsString("unknown") => Unknown
          case JsString("at_home") => AtHome
          case JsString("away") => Away
          case JsString("in_room") => (fields("floor"), fields("room")) match {
            case (JsString(floorName), JsString(roomName)) => InRoom(floorName, roomName)
          }
        }
      }

      override def write(obj: UserPosition): JsValue = obj match {
        case Unknown => JsObject("type" -> JsString("unknown"))
        case AtHome => JsObject("type" -> JsString("at_home"))
        case Away => JsObject("type" -> JsString("away"))
        case InRoom(floorName, roomName) => JsObject("type" -> JsString("in_room"), "floor" -> JsString(floorName), "room" -> JsString(roomName))
      }
    }
  }

  def userPositionSchema: json.Schema[UserPosition] = {
    json.Schema.`object`.apply(Set[json.Schema.`object`.Field[_]](
      json.Schema.`object`.Field("type", json.Json.schema[String], required = true),
      json.Schema.`object`.Field("floor", json.Json.schema[String], required = false),
      json.Schema.`object`.Field("room", json.Json.schema[String], required = false)
    ))
  }

  /** TOPOLOGY DSL **/
  def user(name: String): UserFactory = UserFactory(name)
    .withAttribute(file_attr("avatar", s"$RESOURCE_FOLDER/${name}_avatar.jpg", MediaType.image("jpeg", Compressible, "jpg", "jpeg", "png"), "user_avatar"))
    .withAttribute(var_attr[UserPosition]("position", Unknown, "user_position")(userPositionFormat, userPositionSchema))

  def home(name: String): HomeFactory = HomeFactory(name)

  def floor(name: String, level: Int): FloorFactory = FloorFactory(name, level)
    .withAttribute(file_attr("svg", s"$RESOURCE_FOLDER/$name.svg", ContentTypes.`text/xml(UTF-8)`, "floor_blueprint"))

  def room()(implicit name: sourcecode.Name): RoomFactory = room(name.value)

  def room(name: String): RoomFactory = RoomFactory(name)

  def door(rooms: (RoomFactory, RoomFactory)): DoorFactory = rooms match {
    case (roomA, roomB) => door(roomA.name + "<->" + roomB.name, rooms)
  }

  def door(name: String, rooms: (RoomFactory, RoomFactory)): DoorFactory =
    DoorFactory(name, rooms)

  def window(rooms: (RoomFactory, RoomFactory)): WindowFactory = rooms match {
    case (roomA, roomB) => window(roomA.name + "<->" + roomB.name, rooms)
  }

  def window(name: String, rooms: (RoomFactory, RoomFactory)): WindowFactory =
    WindowFactory(name, rooms)

  def ble_beacon(mac: MacAddress, secretKey: String, user: UserFactory): BleBeaconFactory =
    BleBeaconFactory(mac, secretKey, user)

  /** PREDEFINED VALUE BASED PROPERTIES **/
  def json_property[T: JsonFormat](name: String, value: () => T, semantic: String): JsonPropertyFactory[T] = JsonPropertyFactory.safeDynamic(name, value, semantic)

  def time_now(): JsonPropertyFactory[Long] = JsonPropertyFactory.safeDynamic("time", () => System.currentTimeMillis, "time")

  def tag[T: JsonFormat](name: String, value: T): JsonPropertyFactory[T] = JsonPropertyFactory.static(name, value, "tag")

  /** PREDEFINED STREAM BASED PROPERTIES **/
  def ble_receiver(name: String, receiverMac: MacAddress)
                  (implicit beaconsFactory: Seq[BleBeaconFactory], brokerConfig: BrokerConfig): JsonPropertyFactory[Seq[BeaconData]] = {
    import model.ble.Formats._
    import utils.RichMap._
    var container = Map[String, BeaconData]() //context
    val beacons = new Lazy(beaconsFactory.map(_.build())) //in order to call build while building
    json_from_mqtt[RawBeaconData](s"scanner/$receiverMac/ble").ignoreFailures.mapValue(receivedRaw => {
      beacons.value.find(_.mac == receivedRaw.addr) match {
        case Some(beacon) if beacon.validate(receivedRaw.advData) =>
          val beaconData = BeaconData(beacon.attachedTo.name, DateTime.now, receivedRaw.rssi)
          container = container.addOrUpdate(beaconData.userName -> beaconData)
        case _ => //nothing
      }
      container.toValueSeq
    }) asJsonProperty(name, "ble_receiver", Seq[BeaconData]())
  }

  def open_closed(name: String, openCode: Int, closedCode: Int)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[OpenCloseData] = {
    import model.mhz433.Formats._
    json_from_mqtt[Raw433MhzData]("scanner/+/433").ignoreFailures.collectValue[OpenCloseData]({
      case data if data.code == openCode => model.mhz433.Open(DateTime.now)
      case data if data.code == closedCode => model.mhz433.Close(DateTime.now)
    }).asJsonProperty(name, "is_open", model.mhz433.Unknown)
  }

  def mqtt_bool(name: String, topic: String, caseTrue: String, caseFalse: String, semantic: String)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[Boolean] =
    payload_from_mqtt(topic).tryMapValue {
      case `caseTrue` => Success(true)
      case `caseFalse` => Success(false)
      case v => Failure(new Exception(s"boolean match error for value $v"))
    } asJsonProperty (name, semantic)

  /** GENERIC PROPERTY UTILS **/
  implicit val system: ActorSystem = ActorSystem()

  implicit class JsonPropertyFactoryImplicit[T: JsonFormat](source: Source[Try[T], _]) {
    def asJsonProperty(name: String, semantic: String): JsonPropertyFactory[T] =
      JsonPropertyFactory.fromStream(name, () => source, semantic, None)

    def asJsonProperty(name: String, semantic: String, initial: T): JsonPropertyFactory[T] =
      JsonPropertyFactory.fromStream(name, () => source, semantic, Some(initial))
  }

  implicit class JsonPropertyDoubleFactoryImplicit(source: Source[Try[Double], _]) {
    def integrate: Source[Try[Double], Any] = source.mapValue(v => (System.currentTimeMillis(), v))
      .scanValue((System.currentTimeMillis(), 0.0))({
        case ((lastTime, energy), (now, watt)) => (now, energy + watt * ((now - lastTime) / 1000.0))
      }).mapValue(_._2)
  }

  /** PROPERTY SOURCES **/
  def payload_from_mqtt(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[String], Future[Done]] =
    MqttSource.payloads(brokerConfig, topics: _*).map(Success.apply)

  def json_from_mqtt[T: JsonFormat](topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[T], Future[Done]] =
    MqttSource.objects[T](brokerConfig, topics: _*)

  def payload_from_http[T: JsonFormat](request: HttpRequest, pollingFreq: FiniteDuration = 1.second): Source[Try[String], Cancellable] =
    HttpSource.bodies(request, pollingFreq)

  def json_from_http[T: JsonFormat](request: HttpRequest, pollingFreq: FiniteDuration = 1.second): Source[Try[T], Cancellable] =
    HttpSource.objects[T](request, pollingFreq)

  def fileReader(name: String, path: String, contentType: ContentType, semantic: String): PropertyFactory =
    FileReaderPropertyFactory(name, path, contentType, semantic)

  /** ACTIONS **/
  def json_action[T: JsonFormat : json.Schema](name: String, run: T => Unit, semantic: String) = JsonActionFactory(name, run, semantic)

  def file_writer(name: String, path: String, contentType: ContentType): ActionFactory =
    FileWriterActionFactory(name, path, contentType)

  def turn(name: String): JsonActionFactory[Boolean] =
    JsonActionFactory[Boolean](name, b => println(s"$name action triggered with $b"), "turn")(implicitly[JsonFormat[Boolean]], json.Json.schema[Boolean])

  def trig(actionName: String): ActionFactory = new ActionFactory {
    override def name: String = actionName

    override protected def oneTimeBuild(): Action = new Action {
      override def name: String = actionName

      override def contentType: ContentType = ContentTypes.NoContentType

      override def sink(implicit executor: ExecutionContext): Sink[ByteString, Future[Try[Done]]] = {
        println(name + " triggered")
        Sink.ignore.mapMaterializedValue(_.map(Success.apply))
      }

      override def semantic: String = "trig"
    }
  }

  /** ATTRIBUTES **/
  def file_attr(name: String, path: String, contentType: ContentType, semantic: String): (PropertyFactory, ActionFactory) =
    (fileReader(name, path, contentType, semantic), file_writer(name, path, contentType))

  def var_attr[T: JsonFormat : json.Schema](name: String, initialValue: T, semantic: String): (PropertyFactory, ActionFactory) = {
    var value: T = initialValue
    (json_property(name, () => value, semantic), json_action[T](name, value = _, "update_" + semantic))
  }
}


object Test extends App {

  import ConfigDsl._

  case class HassSensorState(entity_id: String, last_changed: String, last_updated: String, state: String)

  implicit val hassSensorStateFormat: JsonFormat[HassSensorState] = jsonFormat4(HassSensorState)

  implicit val brokerConfig: BrokerConfig = BrokerConfig("192.168.1.10:1883")
  val hassAuth = Authorization(OAuth2BearerToken(utils.File.read("C:\\Users\\Edo\\Desktop\\jwt.txt").getOrElse("")))
  val garageReq = HttpRequest(uri = "https://hass.brb.dynu.net/api/states/sensor.consumo_garage").withHeaders(hassAuth)
  val lucePcReq = HttpRequest(uri = "https://hass.brb.dynu.net/api/states/light.lampada_edo").withHeaders(hassAuth)


  val mario = user("mario")
  val luigi = user("luigi")
  implicit val beacons = Seq(
    ble_beacon("74daeaac2a2d", "SimpleBLEBroadca", mario),
    ble_beacon("23daeaac2a2d", "AnotherKey", luigi)
  )
  val c1 = ble_receiver("ble", "12dadddc2a2d")
  val c2 = ble_receiver("ble", "23dadddc2a2d")
  val c3 = ble_receiver("ble", "34dadddc2a2d")

  val p3 = open_closed("magneto", openCode = 123, closedCode = 321)

  val external = room().withProperties(c1)
  val hallway = room().withProperties(c2)
  val bedRoom = room().withProperties(c3)


  val h = home("home")(
    floor("floor level", 0)(
      hallway,
      bedRoom
    )
  ).withUsers(mario, luigi)
    .withAction(turn("siren"))

  val consumo_garage = json_from_http[HassSensorState](garageReq)
    .mapValue(_.state.toDouble)
    .asJsonProperty("garage (W)", "power")

  val energia_garage = json_from_http[HassSensorState](garageReq)
    .mapValue(_.state.toDouble).integrate
    .asJsonProperty("garage (J)", "energy")

  door(hallway -> external).withProperties(
    time_now(),
    tag("color", "green"),

    json_from_http[HassSensorState](lucePcReq) collectValue {
      case HassSensorState(_, _, _, "off") => false
      case HassSensorState(_, _, _, "on") => true
    } asJsonProperty ("Scrivania", "state"),

    mqtt_bool("Luce letto", "stat/shelly25_1/POWER2", "ON", "OFF", "light_state"),

    consumo_garage,
    energia_garage,
    open_closed("OPENCLOSE", 1, 2)
  )


  val build: Home = h.build()

  while (true) {
    def printProperty[T](p: Property): Unit = {
      p match {
        case jp: JsonProperty[_] =>
          val state = jp.value match {
            case Failure(exception) => exception match {
              case _: NoSuchElementException => "Unknown"
              case ex => ex.getMessage
            }
            case Success(v) => v.toString
          }
          println(s"${p.name}: $state")
        case p =>  println(s"${p.name}: can't display")
      }

    }

    build.floors.head.rooms.head.gateways.head.properties.foreach(p => {
      printProperty(p)
    })
    printProperty(c1.build())
    Thread.sleep(1000)
  }
}