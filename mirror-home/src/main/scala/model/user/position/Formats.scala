package model.user.position

import spray.json.{JsObject, JsString, JsValue, JsonFormat}

object Formats {
    implicit def userPositionFormat: JsonFormat[UserPosition] = {
      new JsonFormat[UserPosition] {
        override def read(json: JsValue): UserPosition = json match {
          case JsObject(fields) => fields("type") match {
            case JsString("unknown") => Unknown
            case JsString("at_home") => AtHome
            case JsString("away") => Away
            case JsString("in_room") => (fields("floor"), fields("room")) match {
              case (JsString(floorName), JsString(roomName)) => InRoom(floorName, roomName)
            }
          }
        }

        override def write(obj: UserPosition): JsValue = obj match {
          case Unknown => JsObject("type" -> JsString("unknown"))
          case AtHome => JsObject("type" -> JsString("at_home"))
          case Away => JsObject("type" -> JsString("away"))
          case InRoom(floorName, roomName) => JsObject("type" -> JsString("in_room"), "floor" -> JsString(floorName), "room" -> JsString(roomName))
        }
      }
    }

    implicit def userPositionSchema: json.Schema[UserPosition] = {
      json.Schema.`object`.apply(Set[json.Schema.`object`.Field[_]](
        json.Schema.`object`.Field("type", json.Json.schema[String], required = true),
        json.Schema.`object`.Field("floor", json.Json.schema[String], required = false),
        json.Schema.`object`.Field("room", json.Json.schema[String], required = false)
      ))
    }
}
