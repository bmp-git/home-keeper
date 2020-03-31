package config.impl

import model._
import utils.Lazy

/*
  Problem: circular reference in immutable object. Solutions:
   - https://stackoverflow.com/questions/8374010/scala-circular-references-in-immutable-data-types
   - https://stackoverflow.com/questions/13416192/by-name-type-parameters
*/

case class DoorImpl(override val name: String,
                            roomsGetter: Lazy[(Room, Room)],
                            override val properties: Set[Property[_]],
                            override val actions: Set[Action[_]]) extends Door {
  lazy val _rooms: (Room, Room) = roomsGetter.value
  override def rooms: (Room, Room) = _rooms
}
