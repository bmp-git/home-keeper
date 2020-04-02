package config.factory.topology


import config.impl.FloorImpl
import model.Floor
import utils.SetContainer

case class FloorFactory(override val name: String) extends DigitalTwinFactory[Floor] {
  private var rooms = SetContainer[RoomFactory, String](_.name, Set())

  def apply(rooms: RoomFactory*): this.type = withRooms(rooms: _*)

  def withRooms(rooms: RoomFactory*): this.type = {
    this.rooms = this.rooms.add(rooms)
    this
  }

  override def oneTimeBuild(): Floor = FloorImpl(name, rooms.content.map(_.build()), properties.map(_.build()), actions.map(_.build()))
}
