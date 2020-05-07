package config.impl

import model.{Action, Property, Room, Window}
import utils.Lazy

case class WindowImpl(override val name: String,
                              roomsGetter: Lazy[(Room, Room)],
                              override val properties: Set[Property],
                              override val actions: Set[Action]) extends Window {
  lazy val _rooms: (Room, Room) = roomsGetter.value
  override def rooms: (Room, Room) = _rooms
  override def toString: String = s"Room($name, (${rooms._1.name}, ${rooms._2.name}), $properties, $actions)"
}

