package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.{Literal, NumberTermImpl}
import model.{BeaconDataSeq, Home, Property, ReceiverStatus}

class ReceiversArtifact extends Artifact {

  def userBleData(userName: String, home: Home): String = {
    "[" + home.zippedRooms.flatMap({
      case (floor, room) => room.properties.collect {
        case Property(_, beaconsData: BeaconDataSeq, "ble_receiver") if beaconsData.seq.exists(_.user == userName) =>
          val data = beaconsData.seq.find(_.user == userName).get
          s"info(room(${floor.name},${room.name}),${data.last_seen},${data.rssi})"
      }
    }).mkString(",") + "]"
  }

  def bleData(home: Home): Object = {
    val result = "a([" + home.users.map(u => {
      "userdata(" + u.name + "," + userBleData(u.name, home) + ")"
    }).mkString(",") + "])"
    val parsed = Literal.parseLiteral(result)
    parsed.getTerm(0)
  }

  def receivers(home: Home): Seq[ReceiverStatus] = home.zippedRooms.toSeq.flatMap(_._2.properties.find(_.semantic == "receiver_status").map(_.value.asInstanceOf[ReceiverStatus]))

  def receiverCount(home: Home): Int = {
    receivers(home).size
  }

  def receiverOnlineCount(home: Home): Int = {
    receivers(home).count(_.online)
  }

  @OPERATION def init(home: Home): Unit = {
    defineObsProperty("ble_data", bleData(home))
    defineObsProperty("receivers_online", new NumberTermImpl(receiverOnlineCount(home)), new NumberTermImpl(receiverCount(home)))
  }

  @LINK def update(home: Home): Unit = {
    updateObsProperty("ble_data", bleData(home))
    updateObsProperty("receivers_online", new NumberTermImpl(receiverOnlineCount(home)), new NumberTermImpl(receiverCount(home)))
  }
}
