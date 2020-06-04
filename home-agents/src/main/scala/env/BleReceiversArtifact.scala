package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.Literal
import model.{BeaconData, Home, Property}


class BleReceiversArtifact extends Artifact {

  def info(userName: String, home: Home): String = {
    "[" + home.zippedRooms.flatMap({
      case (floor, room) => room.properties.collect {
        case Property(_, beaconsData: Seq[BeaconData], "ble_receiver") if beaconsData.exists(_.user == userName) =>
          val data = beaconsData.find(_.user == userName).get
          s"info(room(${floor.name},${room.name}),${data.last_seen},${data.rssi})"
      }
    }).mkString(",") + "]"
  }

  def compute(home: Home): Object = {
    val result = "a([" + home.users.map(u => {
      "userdata(" + u.name + "," + info(u.name, home) + ")"
    }).mkString(",") + "])"
    val parsed = Literal.parseLiteral(result)
    parsed.getTerm(0)
  }

  @OPERATION def init(home: Home): Unit = {
    defineObsProperty("receivers", compute(home))
  }

  @LINK def update(home: Home): Unit = {
    updateObsProperty("receivers", compute(home))
  }
}
