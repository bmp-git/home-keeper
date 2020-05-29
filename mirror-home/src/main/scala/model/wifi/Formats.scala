package model.wifi

import akka.http.scaladsl.model.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsString, JsValue, JsonFormat}

object Formats {
  implicit def wifiCaptureDataFormat: JsonFormat[WifiCaptureData] = jsonFormat(WifiCaptureData, "mac", "rssi")

  implicit def timedWifiCaptureDataFormat: JsonFormat[TimedWifiCaptureData] = new JsonFormat[TimedWifiCaptureData] {
    override def write(data: TimedWifiCaptureData): JsValue = data match {
      case TimedWifiCaptureData(mac, rssi, lastSeen) => JsObject("mac" -> JsString(mac), "rssi" -> JsNumber(rssi), "last_seen" -> JsNumber(lastSeen.clicks))
    }

    override def read(json: JsValue): TimedWifiCaptureData = {
      val obj = json.asJsObject.fields
      TimedWifiCaptureData(obj("mac").convertTo[String], obj("rssi").convertTo[Double], DateTime(obj("mac").convertTo[Long]))
    }
  }
}
