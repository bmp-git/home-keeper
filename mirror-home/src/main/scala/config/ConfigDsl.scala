package config

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{DateTime, HttpRequest}
import config.factory.ble.BleBeaconFactory
import config.factory.property.{DynamicPropertyTemplate, PropertyFactory, PropertyTemplate}
import config.factory.topology
import config.factory.topology._
import model.Units.MacAddress
import model.ble.{BeaconData, RawBeaconData}
import model.mhz433.Raw433MhzData
import model.{BrokerConfig, Home, Property}
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat
import utils.Lazy

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object ConfigDsl {

  /** TOPOLOGY DSL **/
  def user(name: String): UserFactory = UserFactory(name)

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

  def ble_beacon(mac: MacAddress, secretKey: String, user: UserFactory): BleBeaconFactory =
    BleBeaconFactory(mac, secretKey, user)


  /** UTILS **/
  def load_file(path: String): String = {
    val s = scala.io.Source.fromFile(path)
    val v = s.mkString
    s.close()
    v
  }

  /** PROPERTIES DSL **/
  implicit val system: ActorSystem = ActorSystem()

  /** STATIC PROPERTIES **/
  def time_now(): PropertyFactory[Long] = PropertyFactory.dynamic("time", () => System.currentTimeMillis)

  def tag[T: JsonFormat](name: String, value: T): PropertyFactory[T] = PropertyFactory.static(name, value)

  /** DYNAMIC PROPERTIES **/
  def custom_property[T: JsonFormat : ClassTag](name: String): PropertyTemplate[T] =
    DynamicPropertyTemplate[T](name)

  def boolean[T: JsonFormat : ClassTag](name: String, logic: PartialFunction[T, Boolean]): PropertyTemplate[Boolean] =
    custom_property[T](name).tryMapValue({
      case v if logic.isDefinedAt(v) => Success(logic(v))
      case v => Failure(new Exception(s"boolean match error for value $v"))
    })

  def ble_receiver(name: String, receiverMac: MacAddress)
                  (implicit beaconsFactory: Seq[BleBeaconFactory], brokerConfig: BrokerConfig): PropertyFactory[Seq[BeaconData]] = {
    import model.ble.Formats._
    import utils.RichMap._
    var container = Map[String, BeaconData]() //context
    val beacons = new Lazy(beaconsFactory.map(_.build())) //in order to call build while building
    custom_property[RawBeaconData](name).filter(_.isSuccess).mapValue(receivedRaw => {
      beacons.value.find(_.mac == receivedRaw.addr) match {
        case Some(beacon) if beacon.validate(receivedRaw.advData) =>
          val beaconData = BeaconData(beacon.attachedTo, DateTime.now, receivedRaw.rssi)
          container = container.addOrUpdate(beaconData.user.name -> beaconData)
        case _ => //nothing
      }
      container.toValueSeq
    }).on_mqtt(s"scanner/$receiverMac/ble")
  }

  def open_closed(name: String, openCode: Int, closedCode: Int): PropertyTemplate[Boolean] = {
    import model.mhz433.Formats._
    custom_property[Raw433MhzData](name).filter(_.isSuccess).collectValue({
      case data if data.code == openCode => true
      case data if data.code == closedCode => false
    })
  }
}


object Test extends App {

  import ConfigDsl._


  case class HassSensorState(entity_id: String, last_changed: String, last_updated: String, state: String)

  implicit val hassSensorStateFormat: JsonFormat[HassSensorState] = lazyFormat(jsonFormat4(HassSensorState))

  implicit val brokerConfig: BrokerConfig = BrokerConfig("192.168.1.10:1883")
  val hassAuth = Authorization(OAuth2BearerToken(load_file("C:\\Users\\Edo\\Desktop\\jwt.txt")))
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

  val p3 = open_closed("magneto", openCode = 123, closedCode = 321) on_mqtt "scanner/+/433"

  val external = room().withProperties(c1)
  val hallway = room().withProperties(c2)
  val bedRoom = room().withProperties(c3)

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
    boolean[HassSensorState]("Scrivania", {
      case HassSensorState(_, _, _, "off") => false
      case HassSensorState(_, _, _, "on") => true
    }) on_http lucePcReq,
    boolean[String]("Letto", {
      case "ON" => true
      case "OFF" => false
    }) on_mqtt "stat/shelly25_1/POWER2",
    (custom_property[HassSensorState]("garage") on_http garageReq).map(_.state.toInt),
    custom_property[String]("AAAA") on_mqtt "stat/shelly25_1/POWER2"

  )

  println("A")
  val build: Home = h.build()
  println("Builded home")
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

  val openclose = open_closed("OPENCLOSE", 1, 2) on_mqtt "miotopic"
  while (true) {
    build.floors.head.rooms.head.gateways.head.properties.foreach(p => {
      printProperty(p)
    })
    printProperty(c1.build())
    printProperty(openclose.build())
    Thread.sleep(1000)
  }
}