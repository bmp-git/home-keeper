package config

import model._

//https://stackoverflow.com/questions/8374010/scala-circular-references-in-immutable-data-types
//https://stackoverflow.com/questions/13416192/by-name-type-parameters

class Lazy[T](wrp: => T) {
  lazy val value: T = wrp
}

case class RoomImpl(override val name: String,
                    gatewaysGetter: Lazy[Set[Gateway]],
                    override val properties: Set[Property[_]],
                    override val actions: Set[Action[_]]
                   ) extends Room {
  lazy val _gateways: Set[Gateway] = gatewaysGetter.value

  override def gateways: Set[Gateway] = _gateways

  override def toString: String = s"Room($name, ${gateways.map(_.name)}, $properties, $actions)"
}

trait GatewayImpl extends Gateway {
  def roomsGetter: Lazy[(Room, Room)]

  lazy val _rooms: (Room, Room) = roomsGetter.value

  override def rooms: (Room, Room) = _rooms
}

case class DoorImpl(override val name: String,
                    override val roomsGetter: Lazy[(Room, Room)],
                    override val properties: Set[Property[_]],
                    override val actions: Set[Action[_]]) extends GatewayImpl with Door {
  override def toString: String = s"Door($name, (${rooms._1.name}, ${rooms._2.name}), $properties, $actions)"
}

case class HomeImpl(override val name: String,
                    override val floors: Set[Floor],
                    override val properties: Set[Property[_]],
                    override val actions: Set[Action[_]]) extends Home

case class FloorImpl(override val name: String,
                     override val rooms: Set[Room],
                     override val properties: Set[Property[_]],
                     override val actions: Set[Action[_]]) extends Floor


trait OneTimeFactory[T] {
  private var matBuild: Option[T] = None

  @scala.annotation.tailrec
  final def build(): T = matBuild match {
    case Some(value) => value
    case None => matBuild = Some(oneTimeBuild()); build()
  }

  protected def oneTimeBuild(): T
}

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {
  var properties: Set[Property[_]] = Set[Property[_]]()
  var actions: Set[Action[_]] = Set[Action[_]]()

  def withProperty(property: Property[_]): this.type = {
    properties = properties + property
    this
  }

  def withAction(action: Action[_]): this.type = {
    actions = actions + action
    this
  }

}

case class HomeFactory(name: String) extends DigitalTwinFactory[Home] {
  private var floors = Seq[FloorFactory]()

  def withFloor(floor: FloorFactory): this.type = {
    floors = floors :+ floor
    this
  }

  def apply(floors: FloorFactory*): this.type = {
    floors.foreach(withFloor)
    this
  }

  override def oneTimeBuild(): Home = HomeImpl(name, floors.map(_.build()).toSet, properties, actions)
}

case class FloorFactory(name: String) extends DigitalTwinFactory[Floor] {
  private var rooms = Seq[RoomFactory]()

  def withRoom(room: RoomFactory): this.type = {
    rooms = rooms :+ room
    this
  }

  def apply(rooms: RoomFactory*): this.type = {
    rooms.foreach(withRoom)
    this
  }

  override def oneTimeBuild(): Floor = FloorImpl(name, rooms.map(_.build()).toSet, properties, actions)
}

case class RoomFactory(name: String) extends DigitalTwinFactory[Room] {
  private var gateways = Seq[DoorFactory]()

  def withGateway(gateway: DoorFactory): this.type = {
    gateways = gateways :+ gateway
    this
  }

  override def oneTimeBuild(): Room = RoomImpl(name, new Lazy(gateways.map(_.build()).toSet), properties, actions)
}

case class DoorFactory(name: String, rooms: (RoomFactory, RoomFactory)) extends DigitalTwinFactory[Door] {
  override def oneTimeBuild(): Door = DoorImpl(name, new Lazy((rooms._1.build(), rooms._2.build())), properties, actions)
}


object ConfigDsl {

  def home(name: String): HomeFactory = HomeFactory(name)

  def floor(name: String): FloorFactory = FloorFactory(name)

  def room()(implicit name: sourcecode.Name): RoomFactory = room(name.value)

  def room(name: String): RoomFactory = RoomFactory(name)

  def door(rooms: (RoomFactory, RoomFactory)): DoorFactory = rooms match {
    case (roomA, roomB) =>
      val door = DoorFactory(roomA.name + "<->" + roomB.name, rooms)
      roomA.withGateway(door)
      roomB.withGateway(door)
      door
  }
}


object Test extends App {

  import ConfigDsl._

  val hallway = room()
  val bedRoom = room()

  val h = home("home")(
    floor("floor level")(
      hallway,
      bedRoom
    ),
    floor("first level")
  )

  door(bedRoom -> hallway)

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