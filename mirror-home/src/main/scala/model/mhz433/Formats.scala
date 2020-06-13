package model.mhz433

import spray.json.DefaultJsonProtocol.{jsonFormat, _}
import spray.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat}

import scala.util.Try


object Formats {
  //TODO: fix this
  implicit val raw433MhzDataFormat: JsonFormat[Raw433MhzData] = new JsonFormat[Raw433MhzData] {
    override def write(obj: Raw433MhzData): JsValue = ???

    override def read(json: JsValue): Raw433MhzData = {
      Try(jsonFormat(Raw433MhzData.apply, "code", "pulselength", "proto").read(json)).getOrElse({

        json.asJsObject.fields("RfReceived").asJsObject.fields("Data") match {
          case JsString(data) => Raw433MhzData(data, 0, 0)
        }
      })
    }
  }


  implicit object OpenCloseDataFormat extends RootJsonFormat[OpenCloseData] {
    def write(data: OpenCloseData): JsObject = data match {
      case Close(lastChange) => JsObject("open" -> JsBoolean(false), "last_change" -> JsNumber(lastChange.clicks))
      case Open(lastChange) => JsObject("open" -> JsBoolean(true), "last_change" -> JsNumber(lastChange.clicks))
    }

    def read(value: JsValue): OpenCloseData = ??? // nothing?
  }

}
