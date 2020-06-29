package model

import jason.asSyntax.{Literal, Term}

trait Event {
  def toTerm: Term
}

trait GatewayEvent extends Event {
  def gateway: Gateway

  def rooms: (Room, Room)

  def external: String = if (rooms._1.name == "external" || rooms._2.name == "external") "external" else "internal"

  def eventName: String

  def gatewayType:String = gateway match {
    case Door(_, _, _, _) => "door"
    case Window(_, _, _, _) => "window"
  }

  def template: String = s"event($eventName, $gatewayType, ${'"'}${gateway.name}${'"'}, $external, [room(${rooms._1.floorName}, ${rooms._1.name}), room(${rooms._2.floorName}, ${rooms._2.name})])"

  override def toTerm: Term = Literal.parseLiteral(template)
}

case class GatewayOpenEvent(gateway: Gateway, rooms: (Room, Room)) extends GatewayEvent {
  override def eventName: String = "open"
}

case class GatewayMotionDetectionNearEvent(gateway: Gateway, rooms: (Room, Room)) extends GatewayEvent {
  override def eventName: String = "motion_detection_near"
}

case class MotionDetectionEvent(floor: Floor, room: Room) extends Event {
  override def toTerm: Term = Literal.parseLiteral(s"event(motion_detection, room(${floor.name}, ${room.name}))")
}

case class GetBackHomeEvent(user: User) extends Event {
  override def toTerm: Term = Literal.parseLiteral(s"event(get_back_home, ${user.name})")
}

case class UnknownWifiMacEvent(floor: Floor, room: Room, mac: String) extends Event {
  override def toTerm: Term = Literal.parseLiteral(s"event(unknown_wifi_mac, ${'"'}$mac${'"'}, room(${floor.name}, ${room.name}))")
}

case class ReceiverOfflineEvent(floor: Floor, room: Room) extends Event {
  override def toTerm: Term = Literal.parseLiteral(s"event(receiver_offline, room(${floor.name}, ${room.name}))")
}
