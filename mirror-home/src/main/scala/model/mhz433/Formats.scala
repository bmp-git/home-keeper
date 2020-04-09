package model.mhz433

import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol.jsonFormat
import spray.json.RootJsonFormat

object Formats {
  implicit val raw433MhzData: RootJsonFormat[Raw433MhzData] =
    jsonFormat(Raw433MhzData.apply, "code", "pulselength", "proto")
}
