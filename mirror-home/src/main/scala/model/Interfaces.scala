//Room, Door, Window, External env, Home, Floor,
//Beacon, Pir, Videocamera, Perimetrali
package model

import akka.Done
import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsonFormat, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait Property {
  def name: String

  def contentType: ContentType

  def source(implicit executor: ExecutionContext): Try[Source[ByteString, Any]]

  def semantic: String

  def confidence: Double = 1

  def jsonDescription: JsObject = {
    case class Obj(name: String, contentType: String, semantic: String, confidence: Double)
    val wrapperFormat: JsonFormat[Obj] = jsonFormat(Obj, "name", "content-type", "semantic", "confidence")
    wrapperFormat.write(Obj(name, contentType.toString(), semantic, confidence)).asJsObject
  }
}

trait Action {
  def name: String

  def contentType: ContentType

  def sink(implicit executor: ExecutionContext): Sink[ByteString, Future[Try[Done]]]

  def jsonDescription: JsObject = {
    case class Obj(name:String, contentType: String)
    val wrapperFormat: JsonFormat[Obj] = jsonFormat(Obj, "name", "content-type")
    wrapperFormat.write(Obj(name, contentType.toString())).asJsObject
  }
}

trait JsonProperty[T] extends Property {

  def value: Try[T]

  def jsonFormat: JsonFormat[T]

  override def contentType: ContentType = ContentTypes.`application/json`

  override def jsonDescription: JsObject =
    value match {
      case Failure(exception) =>
        JsObject(super.jsonDescription.fields + ("error" -> JsString(exception.getMessage)))
      case Success(v) => JsObject(super.jsonDescription.fields + ("value" -> jsonFormat.write(v)))
    }

  override def source(implicit executor: ExecutionContext): Try[Source[ByteString, Any]] =
    Success(Source.single(ByteString(jsonDescription.compactPrint)))
}

trait JsonAction[T] extends Action {
  //Action[T] must receive a json formatted like this: {"value": `a T value` }

  def trig(t: T): Unit

  override def sink(implicit executor: ExecutionContext): Sink[ByteString, Future[Try[Done]]] = {
    Sink.fold[ByteString, ByteString](ByteString())((a, b) => a.concat(b)).mapMaterializedValue(_.map(content => {
        case class Wrapper[K](value: K)
        implicit val implicitJsonFormat: JsonFormat[T] = jsonFormat
        val wrapperFormat: JsonFormat[Wrapper[T]] = jsonFormat1[T, Wrapper[T]](v => Wrapper(v))
        Try(wrapperFormat.read(JsonParser(ParserInput(content.utf8String)))).map(_.value) match {
          case Failure(exception) => Failure(exception)
          case Success(value) =>
            trig(value)
            Success(Done)
        }
      }))
  }

  //TODO: check if is feasible to add a json schema
  override def jsonDescription: JsObject =
    JsObject(super.jsonDescription.fields + ("schema" -> JsNumber(1)))


  def contentType: ContentType = ContentTypes.`application/json`

  def jsonFormat: JsonFormat[T]
}

trait DigitalTwin { //DigitalTwin situated
  def name: String
  def properties: Set[Property]
  def actions: Set[Action]
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

  def level: Int
}

trait Home extends DigitalTwin {
  def floors: Set[Floor]
}

