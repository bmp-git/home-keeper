package env

import cartago.{Artifact, LINK, OPERATION}
import config.Server
import jason.asSyntax.Literal
import model.{BeaconData, Home, Property}
import play.api.libs.json.Json
import sttp.client.quick.quickRequest
import sttp.model.Uri
import sttp.client.quick._

import scala.util.Try

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

  @OPERATION def updateUserHomePosition(user: String, room: String): Unit = {
    val rLiteral = Literal.parseLiteral(room)
    val floorName = rLiteral.getTerm(0).toString
    val roomName = rLiteral.getTerm(1).toString
    val js = Json.obj("type" -> "in_room", "floor" -> floorName, "room" -> roomName)

    postUserPosition(user, js.toString)
  }

  @OPERATION def updateUserPosition(user: String, place: String): Unit = {
    val js = Json.obj("type" -> place)
    postUserPosition(user, js.toString)
  }

  private def postUserPosition(user: String, body: String): Unit = {
    val actionUrl = s"${Server.uri}/api/home/users/$user/actions/position"
    println(s"UPDATE_POSITION on $actionUrl ...")
    Uri.parse(s"$actionUrl") match {
      case Left(_) =>  println(s"Failed to parse uri $actionUrl")
      case Right(value) => Try(quickRequest.body(body).post(value).send())
    }
  }
}
