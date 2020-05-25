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
         actions <- arrayOf[Action]("actions").apply(data);
         properties <- arrayOf[Property]("properties").apply(data))
      yield (name, properties.toSet, actions.toSet)

  implicit def propertyReads: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         value <- anyStr("value")(data);
         semantic <- str("semantic")(data)) yield Property(name, value, semantic)

  implicit def actionReads: JsonUnmarshaller[Action] = data =>
    for (name <- str("name")(data);
         semantic <- str("semantic")(data)) yield Action(name, semantic)

  implicit def doorReads: JsonUnmarshaller[Door] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Door(name, properties, actions)

  implicit def windowReads: JsonUnmarshaller[Window] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Window(name, properties, actions)

  implicit def roomReads: JsonUnmarshaller[Room] = data =>
    for ((name, properties, actions) <- dtFields(data);
         doors <- arrayOf[Door]("doors").apply(data);
         windows <- arrayOf[Window]("windows").apply(data))
      yield Room(name, properties, actions, doors.toSet, windows.toSet)

  implicit def floorReads: JsonUnmarshaller[Floor] = data =>
    for ((name, properties, actions) <- dtFields(data);
         rooms <- arrayOf[Room]("rooms").apply(data);
         level <- int("level")(data))
      yield Floor(name, properties, actions, rooms.toSet, level)

  implicit def userReads: JsonUnmarshaller[User] = data =>
    for ((name, properties, actions) <- dtFields(data))
      yield User(name, properties, actions)

  def homeParser: JsonUnmarshaller[Home] = data =>
    for ((name, properties, actions) <- dtFields(data);
         floors <- arrayOf[Floor]("floors").apply(data);
         users <- arrayOf[User]("users").apply(data))
      yield Home(name, properties, actions, floors.toSet, users.toSet)
}


case class Property(name: String, value: String, semantic: String)

case class Action(name: String, semantic: String)

trait DigitalTwin {
  def name: String

  def properties: Set[Property]

  def actions: Set[Action]
}

trait Gateway extends DigitalTwin

case class Door(name: String, properties: Set[Property], actions: Set[Action]) extends Gateway

case class Window(name: String, properties: Set[Property], actions: Set[Action]) extends Gateway

case class Room(name: String, properties: Set[Property], actions: Set[Action], doors: Set[Door], windows: Set[Window]) extends DigitalTwin

case class Floor(name: String, properties: Set[Property], actions: Set[Action], rooms: Set[Room], level: Int) extends DigitalTwin

case class User(name: String, properties: Set[Property], actions: Set[Action]) extends DigitalTwin

case class Home(name: String, properties: Set[Property], actions: Set[Action], floors: Set[Floor], users: Set[User]) extends DigitalTwin {
  def zippedRooms: Set[(Floor, Room)] = floors.flatMap(f => f.rooms.map(r => (f, r)))
  def zippedDoors: Set[(Floor, Room, Door)] =  zippedRooms.flatMap {
    case (floor, room) => room.doors.map(d => (floor, room, d))
  }
  def zippedWindows: Set[(Floor, Room, Window)] =  zippedRooms.flatMap {
    case (floor, room) => room.windows.map(w => (floor, room, w))
  }
}

