package model.user.smartphone

import spray.json.DefaultJsonProtocol.jsonFormat12
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._

object Formats {
  implicit def userLocalizationDataFormat: RootJsonFormat[SmartphoneData] = jsonFormat(SmartphoneData,
    "id",
    "picture_url",
    "full_name",
    "nickname",
    "latitude",
    "longitude",
    "timestamp",
    "accuracy",
    "address",
    "country_code",
    "charging",
    "battery_level")
}
