package config.factory.topology


import config.impl.FloorImpl
import model.Floor
import utils.SetContainer

case class FloorFactory(override val name: String, level: Int) extends DigitalTwinFactory[Floor] {
  private var rooms = SetContainer[RoomFactory, String](_.name, Set())

  def apply(rooms: RoomFactory*): this.type = withRooms(rooms: _*)

  //TODO: check if every name of every DT of this floor is unique
  def withRooms(rooms: RoomFactory*): this.type = {
    this.rooms = this.rooms.add(rooms)
    this
  }

  override def oneTimeBuild(): Floor = FloorImpl(name, level, rooms.content.map(_.build()), properties.map(_.build()), actions.map(_.build()))
}
