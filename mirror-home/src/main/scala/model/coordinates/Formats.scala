package model.coordinates

import spray.json.DefaultJsonProtocol.{jsonFormat2, _}
import spray.json.JsonFormat

object Formats {
  implicit def coordinatesFormat: JsonFormat[Coordinates] = jsonFormat2(Coordinates.apply)
}
