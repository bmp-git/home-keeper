//Room, Door, Window, External env, Home, Floor,
//Beacon, Pir, Videocamera, Perimetrali
package model

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsValue, JsonFormat}

import scala.util.{Failure, Success, Try}


trait Property[T] {
  def name: String

  def value: Try[T]

  def contentType: ContentType

  def serialized: Try[Source[ByteString, Any]]
}

trait JsonProperty[T] extends Property[T] {

  override def contentType: ContentType = ContentTypes.`application/json`

  override def serialized: Try[Source[ByteString, Any]] = value match {
    case Success(value) => Success(Source.single(ByteString(JsObject((name, jsonFormat.write(value))).compactPrint)))
    case Failure(exception) => Failure(exception)
  }

  def valueToJson: JsValue = value match {
    case Failure(exception) => JsObject(("error", exception.getMessage.toJson))
    case Success(v) => jsonFormat.write(v)
  }

  def jsonFormat: JsonFormat[T]
}

trait Action[T] {
  def name: String

  def trig(t: T): Unit

  def deserialize(source: String): Try[T]

  def contentType: ContentType
}

trait JsonAction[T] extends Action[T] {
  //Action[Int] <== deserialize == {"value": 10 }

  override def deserialize(source: String): Try[T] = {
    case class Wrapper[K](value:K)
    implicit val implicitJsonFormat: JsonFormat[T] = jsonFormat
    val wrapperFormat:JsonFormat[Wrapper[T]] = jsonFormat1[T, Wrapper[T]](v => Wrapper(v))
    Try(wrapperFormat.read(JsonParser(ParserInput(source)))).map(_.value)
  }
  def contentType: ContentType = ContentTypes.`application/json`

  def jsonFormat: JsonFormat[T]
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

