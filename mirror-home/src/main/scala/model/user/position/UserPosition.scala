package model.user.position

sealed trait UserPosition

case object Unknown extends UserPosition

case object AtHome extends UserPosition

case object Away extends UserPosition

case class InRoom(floorName: String, roomName: String) extends UserPosition
