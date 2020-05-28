package model

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
      yield Room(name, properties, actions, doors.toSet, windows.toSet, s"/api/home/floors/$floorName/rooms/$name")

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

trait Gateway extends DigitalTwin

case class Door(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Window(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Room(name: String, properties: Set[Property], actions: Set[Action], doors: Set[Door], windows: Set[Window], url: String) extends DigitalTwin

case class Floor(name: String, properties: Set[Property], actions: Set[Action], rooms: Set[Room], level: Int, url: String) extends DigitalTwin

case class User(name: String, properties: Set[Property], actions: Set[Action], url: String) extends DigitalTwin

case class Home(name: String, properties: Set[Property], actions: Set[Action], floors: Set[Floor], users: Set[User], url: String) extends DigitalTwin {
  def zippedRooms: Set[(Floor, Room)] = floors.flatMap(f => f.rooms.map(r => (f, r)))

  def zippedDoors: Set[(Floor, Room, Door)] = zippedRooms.flatMap {
    case (floor, room) => room.doors.map(d => (floor, room, d))
  }

  def zippedWindows: Set[(Floor, Room, Window)] = zippedRooms.flatMap {
    case (floor, room) => room.windows.map(w => (floor, room, w))
  }
}

case class BeaconData(user: String, last_seen: Long, rssi: Int)