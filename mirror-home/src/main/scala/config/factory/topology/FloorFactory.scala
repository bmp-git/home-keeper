package config.factory.topology


import config.impl.FloorImpl
import model.Floor
import utils.SetContainer

case class FloorFactory(override val name: String, level: Int) extends DigitalTwinFactory[Floor] {
  private var roomsSet = SetContainer[RoomFactory](Set(), Seq(_.name))

  def apply(rooms: RoomFactory*): this.type = this.rooms(rooms: _*)

  //TODO: check if every name of every DT of this floor is unique?
  def rooms(rooms: RoomFactory*): this.type = {
    this.roomsSet = this.roomsSet.add(rooms)
    this
  }

  override def oneTimeBuild(): Floor = FloorImpl(name, level, roomsSet.content.map(_.build()), propertiesSet.map(_.build()), actionsSet.map(_.build()))
}
