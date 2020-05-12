package model.mhz433

import spray.json.DefaultJsonProtocol.{jsonFormat, _}
import spray.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

object Formats {
  implicit val raw433MhzDataFormat: RootJsonFormat[Raw433MhzData] =
    jsonFormat(Raw433MhzData.apply, "code", "pulselength", "proto")

  implicit object OpenCloseDataFormat extends RootJsonFormat[OpenCloseData] {
    def write(data: OpenCloseData): JsObject = data match {
      case Close(lastChange) => JsObject("open" -> JsBoolean(false), "last_change" -> JsNumber(lastChange.clicks))
      case Open(lastChange) => JsObject("open" -> JsBoolean(true), "last_change" -> JsNumber(lastChange.clicks))
      case Unknown => JsObject()
    }

    def read(value: JsValue): OpenCloseData = ??? // nothing?
  }

}
