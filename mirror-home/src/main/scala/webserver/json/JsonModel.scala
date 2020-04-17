package webserver.json

import config.ConfigDsl
import model.{Action, DigitalTwin, Door, Floor, Gateway, Home, JsonProperty, Property, Room, Window}
import spray.json.{JsValue, _}

final case class LoginRequest(name:String, password:String)
final case class LoginResponse(token:String)
final case class JwtClaimData(user: String)

object JsonModel extends DefaultJsonProtocol {
  implicit val credentialsFormat = jsonFormat2(LoginRequest)
  implicit val jwtClaimDataFormat = jsonFormat1(JwtClaimData)

  private def propertiesField[T <: DigitalTwin](dt:T): JsField = {
    "properties" -> JsArray(dt.properties.map(property).toVector)
  }
  private def nameField[T <: DigitalTwin](dt:T): JsField = {
    "name" -> JsString(dt.name)
  }
  private def actionsField[T <: DigitalTwin](dt:T): JsField = {
    "actions" -> JsArray(dt.actions.map(action).toVector)
  }
  private def digitalTwinJson[T <: DigitalTwin](dt:T): List[JsField] = {
    List(
      nameField(dt),
      propertiesField(dt),
      actionsField(dt)
    )
  }

  private def floorsField(h: Home): JsField = {
    "floors" -> JsArray(h.floors.map(_.toJson).toVector)
  }
  private def roomsField(e: Either[Gateway, Floor]): JsField = e match {
    case Right(floor) => "rooms" -> JsArray(floor.rooms.map(_.toJson).toVector)
    case Left(gateway) => "rooms" -> JsArray(gateway.rooms._1.name.toJson, gateway.rooms._2.name.toJson)
  }
  private def doorsField(r: Room): JsField = {
    "doors" -> JsArray(r.gateways.filter(_.isInstanceOf[Door]).map(_.toJson).toVector)
  }
  private def windowsField(r: Room): JsField = {
    "windows" -> JsArray(r.gateways.filter(_.isInstanceOf[Window]).map(_.toJson).toVector)
  }

  def property(property: Property[_]): JsObject = {
    property match {
      case p: JsonProperty[_] => JsObject((property.name, p.valueToJson))
      case _ => JsObject((property.name, JsString("can't display"))) //TODO: verify
    }
  }

  def properties[T <: DigitalTwin](dt:T): JsObject = {
    JsObject(propertiesField(dt))
  }

  def action(action: Action[_]): JsObject = {
    JsObject(("name", action.name.toJson))
  }
  def actions[T <: DigitalTwin](dt:T): JsObject = {
    JsObject(actionsField(dt))
  }

  def floors(h: Home): JsObject = {
    JsObject(floorsField(h))
  }
  def rooms(e: Either[Gateway, Floor]): JsObject = {
    JsObject(roomsField(e))
  }
  def doors(r: Room): JsObject = {
    JsObject(doorsField(r))
  }
  def windows(r: Room): JsObject = {
    JsObject(windowsField(r))
  }


  implicit def roomFormat: JsonWriter[Room] = (room: Room) => {
    JsObject( digitalTwinJson(room) :+ doorsField(room) :+ windowsField(room) : _* )
  }
  implicit def gatewayFormat: JsonFormat[Gateway] = lazyFormat(new JsonFormat[Gateway] {
    def write(gateway: Gateway): JsValue = {
      JsObject ( digitalTwinJson(gateway) :+ roomsField(Left(gateway)) :_* )
    }

    override def read(json: JsValue): Gateway = ???
  })
  implicit def floorFormat: JsonWriter[Floor] = (floor: Floor) => {
    JsObject( digitalTwinJson(floor) :+ roomsField(Right(floor)) :_* )
  }
  implicit def homeFormat: JsonWriter[Home] = (home: Home) => {
    JsObject( digitalTwinJson(home) :+ floorsField(home): _* )
  }
}

object Examples extends App {
  import JsonModel._
  import ConfigDsl._

  val external = room()
  val hallway = room()
  val bedRoom = room()

  val h = home("home")(
    floor("floor level")(
      hallway,
      bedRoom
    )
  )

  door(bedRoom -> hallway).withProperties(time_now())
  door(hallway -> external)
  val b = h.build()
  while(true) {
    Thread.sleep(1000)
    println(b.toJson)
  }
}