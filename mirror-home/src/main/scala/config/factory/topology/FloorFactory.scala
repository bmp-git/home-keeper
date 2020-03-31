package config.factory.topology

import config.impl.FloorImpl
import model.Floor

case class FloorFactory(name: String) extends DigitalTwinFactory[Floor] {
  private var rooms = Seq[RoomFactory]()

  def apply(rooms: RoomFactory*): this.type = {
    this.withRooms(rooms:_*)
    this
  }

  def withRooms(rooms: RoomFactory*): this.type = {
    this.rooms = this.rooms ++ rooms
    this
  }

  override def oneTimeBuild(): Floor = FloorImpl(name, rooms.map(_.build()).toSet, properties.map(_.build()), actions)
}
