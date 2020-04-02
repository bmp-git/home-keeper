package webserver.model

import config.ConfigDsl
import model.{DigitalTwin, Door, Floor, Gateway, Home, Room, Window}
import spray.json._

object JsonModel extends DefaultJsonProtocol {
  private def digitalTwinJson[T <: DigitalTwin](dt:T): List[JsField] = {
    List(
      "name" -> JsString(dt.name),
      "properties" -> JsArray(dt.properties.map(p => JsObject((p.name, p.valueToJson))).toVector),
      "actions" -> JsArray(dt.actions.map(_.name.toJson).toVector)
    )
  }

  implicit def roomFormat: JsonWriter[Room] = (room: Room) => {
    JsObject(
      digitalTwinJson(room) :+
        ("doors" -> JsArray(room.gateways.filter(_.isInstanceOf[Door]).map(_.toJson).toVector)) :+
        ("windows" -> JsArray(room.gateways.filter(_.isInstanceOf[Window]).map(_.toJson).toVector)): _*
    )
  }

  implicit def gatewayFormat: JsonFormat[Gateway] = lazyFormat(new JsonFormat[Gateway] {
    def write(gateway: Gateway): JsValue = {
      JsObject (
        digitalTwinJson(gateway) :+
          ("rooms" -> JsArray(gateway.rooms._1.name.toJson, gateway.rooms._2.name.toJson)):_*
      )
    }

    override def read(json: JsValue): Gateway = ???
  })

  implicit def floorFormat: JsonWriter[Floor] = (floor: Floor) => {
    JsObject(
      digitalTwinJson(floor) :+
        ("rooms" -> JsArray(floor.rooms.map(_.toJson).toVector)): _*,
    )
  }

  implicit def homeFormat: JsonWriter[Home] = (home: Home) => {
    JsObject(
      digitalTwinJson(home) :+
        ("floors" -> JsArray(home.floors.map(_.toJson).toVector)): _*,
    )
  }
}

object Examples extends App {
  import JsonModel._
  import ConfigDsl._

  val external = room()
  val hallway = room()
  val bedRoom = room()

  val h = home("home")(
    floor("floor level")(
      hallway,
      bedRoom
    )
  )

  door(bedRoom -> hallway).withProperties(time_now())
  door(hallway -> external)
  val b = h.build()
  while(true) {
    Thread.sleep(1000)
    println(b.toJson)
  }
}