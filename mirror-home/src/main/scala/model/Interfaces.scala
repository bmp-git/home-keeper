//Room, Door, Window, External env, Home, Floor,
//Beacon, Pir, Videocamera, Perimetrali
package model

import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsValue, JsonFormat}

import scala.util.{Failure, Success, Try}


trait Property[T] {
  def name: String

  def value: Try[T]

  def jsonFormat: JsonFormat[T]

  def valueToJson: JsValue = value match {
    case Failure(exception) => JsObject(("error", exception.getMessage.toJson))
    case Success(v) => jsonFormat.write(v)
  }
}

trait Action[T] {
  def name: String

  def trig(t: T): Unit //Unit or Option[Exception] or Future[Try[Done]]?

  def jsonFormat: JsonFormat[T]

  def trigFromJson(jsValue: JsValue): Try[Unit] =
    Try(jsonFormat.read(jsValue)).map(v => trig(v))
}

trait DigitalTwin { //DigitalTwin situated
  def name: String
  def properties: Set[Property[_]]
  def actions: Set[Action[_]]
}

trait User extends DigitalTwin {

}

//Home topology
trait Gateway extends DigitalTwin {
  def rooms: (Room, Room)
}

trait Door extends Gateway
trait Window extends Gateway
trait Room extends DigitalTwin {
  def gateways: Set[Gateway]
}
trait Floor extends DigitalTwin {
  def rooms: Set[Room]
}
trait Home extends DigitalTwin {
  def floors: Set[Floor]
}

