package model.ble

import spray.json.DefaultJsonProtocol._
import spray.json._

object Formats {
  implicit val rawBeaconData: RootJsonFormat[RawBeaconData] =
    jsonFormat(RawBeaconData.apply, "addr", "rssi", "adv_data")

  implicit object BeaconDataJsonFormat extends RootJsonFormat[BeaconData] {
    def write(data: BeaconData) = JsObject(
      "rssi" -> JsNumber(data.rssi),
      "user" -> JsString(data.user.name),
      "last_seen" -> JsNumber(data.lastSeen.clicks),
    )

    def read(value: JsValue): BeaconData = ??? // nothing?
  }
}
