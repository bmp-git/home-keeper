package model.user.smartphone

import spray.json.DefaultJsonProtocol.jsonFormat12
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._

object Formats {
  implicit def userLocalizationDataFormat: RootJsonFormat[SmartphoneData] = jsonFormat12(SmartphoneData.apply)
}
