package webserver.json

import config.ConfigDsl
import model._
import spray.json.{JsValue, _}

final case class LoginRequest(name:String, password:String)
final case class LoginResponse(token:String)
final case class JwtClaimData(user: String)

object JsonModel extends DefaultJsonProtocol {
  implicit val credentialsFormat = jsonFormat2(LoginRequest)
  implicit val jwtClaimDataFormat = jsonFormat1(JwtClaimData)

  def dtFields[T <: DigitalTwin](dt: T): List[JsField] = {
    List(
      "name" -> JsString(dt.name),
      "properties" -> propertiesJsArray(dt),
      "actions" -> actionsJsArray(dt)
    )
  }

  def propertiesJsArray[T <: DigitalTwin](dt: T): JsArray = JsArray(dt.properties.map(_.jsonDescription).toVector)

  def actionsJsArray[T <: DigitalTwin](dt: T): JsArray = JsArray(dt.actions.map(_.jsonDescription).toVector)

  def floorsJsArray(h: Home): JsArray = JsArray(h.floors.map(_.toJson).toVector)

  def roomsJsArray(e: Either[Gateway, Floor]): JsArray = e match {
    case Right(floor) => JsArray(floor.rooms.map(_.toJson).toVector)
    case Left(gateway) => JsArray(gateway.rooms._1.name.toJson, gateway.rooms._2.name.toJson)
  }

  def doorsJsArray(r: Room): JsArray = JsArray(r.gateways.filter(_.isInstanceOf[Door]).map(_.toJson).toVector)

  def windowsJsArray(r: Room): JsArray = JsArray(r.gateways.filter(_.isInstanceOf[Window]).map(_.toJson).toVector)

  implicit def roomFormat: JsonWriter[Room] = (room: Room) => {
    JsObject(dtFields(room) :+ ("doors" -> doorsJsArray(room)) :+ ("windows" -> windowsJsArray(room)): _*)
  }
  implicit def gatewayFormat: JsonFormat[Gateway] = lazyFormat(new JsonFormat[Gateway] {
    def write(gateway: Gateway): JsValue = {
      JsObject(dtFields(gateway) :+ ("rooms" -> roomsJsArray(Left(gateway))): _*)
    }

    def read(json: JsValue): Gateway = throw new NotImplementedError()
  })
  implicit def floorFormat: JsonWriter[Floor] = (floor: Floor) => {
    JsObject(dtFields(floor) :+ ("rooms" -> roomsJsArray(Right(floor))) :+ ("level" -> JsNumber(floor.level)): _*)
  }
  implicit def homeFormat: JsonWriter[Home] = (home: Home) => {
    JsObject(dtFields(home) :+ ("floors" -> floorsJsArray(home)): _*)
  }
}