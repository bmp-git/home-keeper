package model.ble

import spray.json.DefaultJsonProtocol._
import spray.json._

object Formats {
  implicit val rawBeaconData: RootJsonFormat[RawBeaconData] =
    jsonFormat(RawBeaconData.apply, "addr", "rssi", "adv_data")

  implicit object BeaconDataJsonFormat extends RootJsonFormat[BeaconData] {
    def write(data: BeaconData): JsObject = JsObject(
      "rssi" -> JsNumber(data.rssi),
      "user" -> JsString(data.userName),
      "last_seen" -> JsNumber(data.lastSeen.clicks),
    )

    def read(value: JsValue): BeaconData = ??? // nothing?
  }
}
