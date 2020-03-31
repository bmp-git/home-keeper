package config

import config.factory.{topology, _}
import config.factory.topology.{DoorFactory, FloorFactory, HomeFactory, RoomFactory, WindowFactory}

object ConfigDsl {

  def home(name: String): HomeFactory = HomeFactory(name)

  def floor(name: String): FloorFactory = FloorFactory(name)

  def room()(implicit name: sourcecode.Name): RoomFactory = room(name.value)

  def room(name: String): RoomFactory = RoomFactory(name)

  def door(rooms: (RoomFactory, RoomFactory)): DoorFactory = rooms match {
    case (roomA, roomB) =>
      val door = topology.DoorFactory(roomA.name + "<->" + roomB.name, rooms)
      roomA.withGateways(door)
      roomB.withGateways(door)
      door
  }

  def window(rooms: (RoomFactory, RoomFactory)): WindowFactory = rooms match {
    case (roomA, roomB) =>
      val window = topology.WindowFactory(roomA.name + "<->" + roomB.name, rooms)
      roomA.withGateways(window)
      roomB.withGateways(window)
      window
  }
}


object Test extends App {

  import ConfigDsl._

  val external = room()
  val hallway = room()
  val bedRoom = room()

    val h = home("home")(
      floor("floor level")(
        hallway,
        bedRoom
      ),
      floor("first level")
    )

  door(bedRoom -> hallway).withProperties(
    ???,
    ???
  )
  door(hallway -> external)

  val build = h.build()
  println(build)


  /*println(Eval[Unit](
    """
      |import config.ConfigDsl._
      |val hallway = room()
      |val bedRoom = room()
      |val d = door(bedRoom -> hallway)
      |val buildBedRoom = bedRoom.build()
      |val buildD = d.build()
      |println(buildBedRoom)
      |println(buildD)
      |println(buildBedRoom.gateways)
      |println(buildD.rooms)
      |""".stripMargin))*/
}