package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.Literal
import model.{BeaconDataSeq, Home, Property}

class BleReceiversArtifact extends Artifact {

  def info(userName: String, home: Home): String = {
    "[" + home.zippedRooms.flatMap({
      case (floor, room) => room.properties.collect {
        case Property(_, beaconsData: BeaconDataSeq, "ble_receiver") if beaconsData.seq.exists(_.user == userName) =>
          val data = beaconsData.seq.find(_.user == userName).get
          s"info(room(${floor.name},${room.name}),${data.last_seen},${data.rssi})"
      }
    }).mkString(",") + "]"
  }
 // movement("external", _) ~40.seconds~> gateway_open("external", gateway) ~10.seconds~> movement("internal", room) -> if gateway is in room
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
