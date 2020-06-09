package model


import jason.asSyntax.{Literal, Term}
import play.api.libs.json._

object Unmarshallers {
  type Unmarshaller[-I, +O] = I => Option[O]
  type JsonUnmarshaller[+O] = Unmarshaller[JsValue, O]

  implicit class RichUnmarshaller[I, O](p: Unmarshaller[I, O]) {
    def map[T](f: O => T): Unmarshaller[I, T] = i => p(i).map(f)
  }

  implicit def fromJsLookupResultToOption(res: JsLookupResult): Option[JsValue] = Some(res) collect {
    case JsDefined(value) => value
  }

  def extract[T: Reads]: JsonUnmarshaller[T] = _.asOpt[T]

  def str(name: String): JsonUnmarshaller[String] = value[String](name)

  def number(name: String): JsonUnmarshaller[BigDecimal] = value[BigDecimal](name)

  def value[T: Reads](name: String): JsonUnmarshaller[T] = data => json(name)(data) flatMap (_.asOpt[T])

  def json(name: String): JsonUnmarshaller[JsValue] = _ \ name

  def long(name: String): JsonUnmarshaller[Long] = value[Long](name)

  def int(name: String): JsonUnmarshaller[Int] = value[Int](name)

  def double(name: String): JsonUnmarshaller[Double] = value[Double](name)

  def bool(name: String): JsonUnmarshaller[Boolean] = value[Boolean](name)

  def first[I, O](parsers: Seq[I => Option[O]]): Unmarshaller[I, O] = input =>
    (for (parser <- parsers; result <- parser(input)) yield result).headOption

  def array(name: String): JsonUnmarshaller[Seq[JsValue]] = data => data(name) match {
    case JsArray(value) => Some(value)
    case _ => None
  }

  def arrayOf[T: JsonUnmarshaller](name: String): JsonUnmarshaller[Seq[T]] = data => data(name) match {
    case JsArray(value) => Some(value.map(implicitly[JsonUnmarshaller[T]]).collect {
      case Some(v) => v
    })
    case _ => None
  }

  def anyStr(name: String): JsonUnmarshaller[String] = data => json(name)(data).map {
    case JsString(body) => body
    case a => a.toString
  }

  def dtFields: JsonUnmarshaller[(String, Set[Property], Set[Action])] = data =>
    for (name <- str("name")(data);
         actions <- arrayOf[Action]("actions")(actionUnmarshaller)(data);
         properties <- arrayOf[Property]("properties")(anyPropertyUnmarshaller).apply(data))
      yield (name, properties.toSet, actions.toSet)

  def anyPropertyUnmarshaller: JsonUnmarshaller[Property] =
    first(Seq(
      bleReceiverPropertyUnmarshaller,
      timePropertyUnmarshaller,
      locationPropertyUnmarshaller,
      smartphonePropertyUnmarshaller,
      userPositionPropertyUnmarshaller,
      isOpenPropertyUnmarshaller,
      motionDetectionPropertyUnmarshaller,
      unknownPropertyUnmarshaller))

  def beaconDataUnmarshaller: JsonUnmarshaller[BeaconData] = data =>
    for (name <- str("user")(data);
         rssi <- int("rssi")(data);
         last_seen <- long("last_seen")(data))
      yield BeaconData(name, last_seen, rssi)
  
  def beaconDataSeqUnmarshaller: JsonUnmarshaller[Seq[BeaconData]] = {
    case JsArray(value) =>
      Some(value.map(beaconDataUnmarshaller).collect {
        case Some(v) => v
      })
    case _ => None
  }

  def coordinatesUnmarshaller: JsonUnmarshaller[Coordinates] = data => {
    for(latitude <- double("latitude")(data);
        longitude <- double("longitude")(data)) yield Coordinates(latitude, longitude)
  }

  def smartphoneUnmarshaller: JsonUnmarshaller[SmartphoneData] = data => {
    for(latitude <- double("latitude")(data);
        longitude <- double("longitude")(data);
        timestamp <- long("timestamp")(data);
        accuracy <- int("accuracy")(data)) yield SmartphoneData(latitude, longitude, timestamp, accuracy)
  }

  def userPositionUnmarshaller: JsonUnmarshaller[UserPosition] = {
    first(Seq[JsonUnmarshaller[UserPosition]](
      data => for ("unknown" <- str("type")(data)) yield Unknown,
      data => for ("at_home" <- str("type")(data)) yield AtHome,
      data => for ("away" <- str("type")(data)) yield Away,
      data => for ("in_room" <- str("type")(data);
                   room <- str("room")(data);
                   floor <- str("floor")(data)) yield InRoom(room, floor)))
  }

  def openCloseDataUnmarshaller: JsonUnmarshaller[Option[OpenCloseData]] = {
    first(Seq[JsonUnmarshaller[Option[OpenCloseData]]](
      data => for (true <- bool("open")(data); time <- long("last_change")(data)) yield Some(Open(time)),
      data => for (false <- bool("open")(data); time <- long("last_change")(data)) yield Some(Close(time)),
      _ => Some(None)
    ))
  }

  def motionDetectionUnmarshaller: JsonUnmarshaller[Option[MotionDetection]] = {
    first(Seq[JsonUnmarshaller[Option[MotionDetection]]](
      data => for (time <- long("last_seen")(data)) yield Some(MotionDetection(time)),
      _ => Some(None)
    ))
  }

  def bleReceiverPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "ble_receiver" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- beaconDataSeqUnmarshaller(valueData))
      yield Property(name, value, "ble_receiver")

  def timePropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "time" <- str("semantic")(data);
         value <- str("value")(data)) yield Property(name, value, "time")

  def locationPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "location" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- coordinatesUnmarshaller(valueData)) yield Property(name, value, "location")

  def smartphonePropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "smartphone_data" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- smartphoneUnmarshaller(valueData)) yield Property(name, value, "smartphone_data")

  def userPositionPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "user_position" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- userPositionUnmarshaller(valueData)) yield Property(name, value, "user_position")

  def isOpenPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "is_open" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- openCloseDataUnmarshaller(valueData)) yield Property(name, value, "is_open")

  def motionDetectionPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "motion_detection" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- motionDetectionUnmarshaller(valueData)) yield Property(name, value, "motion_detection")

  def unknownPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         value <- anyStr("value")(data);
         semantic <- str("semantic")(data)) yield Property(name, value, semantic)

  def actionUnmarshaller: JsonUnmarshaller[Action] = data =>
    for (name <- str("name")(data);
         semantic <- str("semantic")(data)) yield Action(name, semantic)

  def doorUnmarshaller(floorName: String, roomName: String): JsonUnmarshaller[Door] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Door(name, properties, actions,
      s"/api/home/floors/$floorName/rooms/$roomName/doors/$name")

  def windowUnmarshaller(floorName: String, roomName: String): JsonUnmarshaller[Window] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Window(name, properties, actions,
      s"/api/home/floors/$floorName/rooms/$roomName/windows/$name")

  def roomUnmarshaller(floorName: String): JsonUnmarshaller[Room] = data =>
    for ((name, properties, actions) <- dtFields(data);
         doors <- arrayOf[Door]("doors")(doorUnmarshaller(floorName, name))(data);
         windows <- arrayOf[Window]("windows")(windowUnmarshaller(floorName, name))(data))
      yield Room(name, floorName, properties, actions, doors.toSet, windows.toSet, s"/api/home/floors/$floorName/rooms/$name")

  def floorUnmarshaller: JsonUnmarshaller[Floor] = data =>
    for ((name, properties, actions) <- dtFields(data);
         rooms <- arrayOf[Room]("rooms")(roomUnmarshaller(name))(data);
         level <- int("level")(data))
      yield Floor(name, properties, actions, rooms.toSet, level, s"/api/home/floors/$name")

  def userUnmarshaller: JsonUnmarshaller[User] = data =>
    for ((name, properties, actions) <- dtFields(data))
      yield User(name, properties, actions, s"/api/home/users/$name")

  def homeParser: JsonUnmarshaller[Home] = data =>
    for ((name, properties, actions) <- dtFields(data);
         floors <- arrayOf[Floor]("floors")(floorUnmarshaller)(data);
         users <- arrayOf[User]("users")(userUnmarshaller)(data))
      yield Home(name, properties, actions, floors.toSet, users.toSet, "/api/home")
}

case class Property(name: String, value: AnyRef, semantic: String)

case class Action(name: String, semantic: String)

trait Remote {
  def url: String
}

trait DigitalTwin extends Remote {
  def name: String

  def properties: Set[Property]

  def actions: Set[Action]
}

trait Gateway extends DigitalTwin {
  def rooms(home: Home): (Room, Room) = {
    home.zippedRooms.filter(r => (r._2.doors ++ r._2.windows).exists(_.name == this.name)).map(_._2).toList match {
      case r1 :: r2 :: Nil => (r1, r2)
    }
  }
}

case class Door(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Window(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Room(name: String, floorName: String, properties: Set[Property], actions: Set[Action], doors: Set[Door], windows: Set[Window], url: String) extends DigitalTwin

case class Floor(name: String, properties: Set[Property], actions: Set[Action], rooms: Set[Room], level: Int, url: String) extends DigitalTwin

case class User(name: String, properties: Set[Property], actions: Set[Action], url: String) extends DigitalTwin

trait Event {
  def toTerm: Term
}

trait GatewayEvent extends Event {
  def gateway: Gateway

  def rooms: (Room, Room)

  def external: String = if (rooms._1.name == "external" || rooms._2.name == "external") "external" else "internal"

  def eventName: String

  def template: String = s"event($eventName, ${'"'}${gateway.name}${'"'}, $external, [room(${rooms._1.floorName}, ${rooms._1.name}), room(${rooms._2.floorName}, ${rooms._2.name})])"

  override def toTerm: Term = Literal.parseLiteral(template)
}

case class DoorOpenEvent(gateway: Gateway, rooms: (Room, Room)) extends GatewayEvent {
  //event(door_open, Name, internal, Rooms)
  override def eventName: String = "door_open"
} //
case class WindowOpenEvent(gateway: Gateway, rooms: (Room, Room)) extends GatewayEvent {
  //event(window_open, Name, external, Rooms)
  override def eventName: String = "window_open"
} //
case class MotionDetectionNearEvent(gateway: Gateway, rooms: (Room, Room)) extends GatewayEvent {
  //event(motion_detection_near, bedroomWindow)
  override def eventName: String = "motion_detection_near"
} //
case class MotionDetectionEvent(floor: Floor, room: Room) extends Event {
  //event(motion_detection, room(firstfloor, bedroom))
  override def toTerm: Term = Literal.parseLiteral(s"event(motion_detection, room(${floor.name}, ${room.name}))")
} //
case class GetBackHomeEvent(user: User) extends Event {
  //event(get_back_home, lorenzomondani)
  override def toTerm: Term = Literal.parseLiteral(s"event(get_back_home, ${user.name})")
} //
case class UnknownWifiMacEvent(floor: Floor, room: Room) extends Event {
  //event(unknown_wifi_mac, room(firstfloor, kitchen))
  override def toTerm: Term = Literal.parseLiteral(s"event(unknown_wifi_mac, room(${floor.name}, ${room.name}))")
} //TODO: implement


case class Home(name: String, properties: Set[Property], actions: Set[Action], floors: Set[Floor], users: Set[User], url: String) extends DigitalTwin {
  def zippedRooms: Set[(Floor, Room)] = floors.flatMap(f => f.rooms.map(r => (f, r)))

  def zippedDoors: Set[(Floor, Room, Door)] = zippedRooms.flatMap {
    case (floor, room) => room.doors.map(d => (floor, room, d))
  }

  def zippedWindows: Set[(Floor, Room, Window)] = zippedRooms.flatMap {
    case (floor, room) => room.windows.map(w => (floor, room, w))
  }

  /*this - old*/
  implicit class RichSeq[T](seq: Seq[T]) {
    def join(other: Seq[T], on: ((T, T)) => Boolean): Seq[(T, T)] = {
      seq.flatMap(t1 => other.map(t2 => (t1, t2))).filter(on)
    }

    def distinctBy[K](f: T => K): Seq[T] = seq.groupBy(f).map(_._2.head).toSeq
  }

  def -(old: Home): Seq[Event] = {
    def gatewayFilter[T](seq: Seq[(Floor, Room, Gateway)], semantic: String): Seq[(Floor, Room, Gateway, T)] = {
      seq.flatMap {
        case (floor, room, door) => door.properties.find(_.semantic == semantic).map(p => (floor, room, door, p.value.asInstanceOf[T]))
      }
    }

    def gatewayWithProperty[PT, ET <: Event](news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)], semantic: String, g: Gateway => ET, changeFilter: (PT, PT) => Boolean): Seq[Event] = {
      val newGateways = gatewayFilter[PT](news, semantic)
      val oldGateways = gatewayFilter[PT](olds, semantic)
      newGateways.join(oldGateways, {
        case ((newFloor, newRoom, newGateway, newProperty), (oldFloor, oldRoom, oldGateway, oldProperty)) if changeFilter(newProperty, oldProperty) =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name && oldGateway.name == newGateway.name
        case _ => false
      }).distinctBy(_._1._3.name).map {
        case ((_, _, gateway, _), (_, _, _, _)) => g(gateway)
      }
    }

    def debounceTimems = 1000

    def gatewayOpened(news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)], g: Gateway => GatewayEvent): Seq[Event] = {
      gatewayWithProperty[Option[OpenCloseData], GatewayEvent](news, olds, "is_open", g, {
        case (Some(Open(_)), Some(Close(_)) | None) => true
        case (Some(Open(nt)), Some(Open(ot))) if nt > ot + debounceTimems=> true
        case (Some(Close(nt)), Some(Close(ot))) if nt > ot + debounceTimems => true
        case _ => false
      })
    }

    def gatewayMotionDetected(news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)]): Seq[Event] = {
      gatewayWithProperty[Option[MotionDetection], MotionDetectionNearEvent](news, olds, "motion_detection", g => MotionDetectionNearEvent(g, g.rooms(this)), {
        case (Some(MotionDetection(newTime)),  Some(MotionDetection(oldTime))) if newTime > oldTime + debounceTimems => true
        case (Some(MotionDetection(_)), None) => true
        case _ => false
      })
    }

    def motionDetectionFilter(seq: Seq[(Floor, Room)]): Seq[(Floor, Room, Option[MotionDetection])] = {
      seq.flatMap {
        case (floor, room) => room.properties.find(_.semantic == "motion_detection").map(p => (floor, room, p.value.asInstanceOf[Option[MotionDetection]]))
      }
    }

    def motionDetection(news: Seq[(Floor, Room)], olds: Seq[(Floor, Room)]): Seq[Event] = {
      val newRooms = motionDetectionFilter(news)
      val oldRooms = motionDetectionFilter(olds)
      newRooms.join(oldRooms, {
        case ((newFloor, newRoom, Some(MotionDetection(newTime))), (oldFloor, oldRoom, Some(MotionDetection(oldTime)))) if newTime > oldTime + debounceTimems =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name
        case ((newFloor, newRoom, Some(MotionDetection(_))), (oldFloor, oldRoom, None)) =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name
        case _ => false
      }).map {
        case ((floor, room, _), (_, _, _)) => MotionDetectionEvent(floor, room)
      }
    }

    def userPositionFilter(users: Seq[User]): Seq[(User, UserPosition)] = {
      users.flatMap(user => user.properties.find(p => p.semantic == "user_position").map(p => (user, p.value.asInstanceOf[UserPosition])))
    }

    def backHomeUser(news: Seq[User], olds: Seq[User]): Seq[Event] = {
      val newUsers = userPositionFilter(news)
      val oldUsers = userPositionFilter(olds)

      newUsers.join(oldUsers, {
        case ((newUser, AtHome | _:InRoom), (oldUser, Unknown | Away)) => newUser.name == oldUser.name
        case _ => false
      }).map {
        case ((user, _), (_,_)) => GetBackHomeEvent(user)
      }
    }


    gatewayOpened(this.zippedDoors.toSeq, old.zippedDoors.toSeq, d => DoorOpenEvent(d, d.rooms(this))) ++
      gatewayOpened(this.zippedWindows.toSeq, old.zippedWindows.toSeq, w => WindowOpenEvent(w, w.rooms(this))) ++
      motionDetection(this.zippedRooms.toSeq, old.zippedRooms.toSeq) ++
      gatewayMotionDetected(this.zippedDoors.toSeq, old.zippedDoors.toSeq) ++
      gatewayMotionDetected(this.zippedWindows.toSeq, old.zippedWindows.toSeq) ++
      backHomeUser(this.users.toSeq, old.users.toSeq)
  }
}

object Test extends App {
  println("Started!")

  import sttp.client.quick._

  var h: Option[Home] = None

  while (true) {
    Thread.sleep(500)
    val response = quickRequest.get(uri"http://localhost:8090/api/home").send()
    val json = Json.parse(response.body)
    (h, Unmarshallers.homeParser(json)) match {
      case (None, Some(home)) => h = Some(home)
      case (Some(oldHome), Some(newHome)) =>
        val events = newHome - oldHome
        events.toSet.foreach(println)
        println("------------------------------------------------------")
        h = Some(newHome)
      case _ => println("!")
    }
  }

}

case class BeaconData(user: String, last_seen: Long, rssi: Int)

case class Coordinates(latitude: Double, longitude: Double)

case class SmartphoneData(latitude: Double,
                          longitude: Double,
                          timestamp: Long,
                          accuracy: Int)


sealed trait UserPosition

case object Unknown extends UserPosition

case object AtHome extends UserPosition

case object Away extends UserPosition

case class InRoom(floorName: String, roomName: String) extends UserPosition


sealed trait OpenCloseData

case class Open(lastChange: Long) extends OpenCloseData

case class Close(lastChange: Long) extends OpenCloseData

case class MotionDetection(lastSeen: Long)