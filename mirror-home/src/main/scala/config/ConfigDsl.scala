package config

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import config.factory.topology._
import config.factory.{topology, _}

import scala.concurrent.duration._

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

  val system: ActorSystem = ActorSystem()

  def time_now(): StaticPropertyFactory[Long] = new StaticPropertyFactory[Long] {
    override def actorSystem: ActorSystem = system

    override def name: String = "time"

    override def input: Source[Long, NotUsed] = Source.tick(0.second, 1.second, None)
      .map(_ => System.currentTimeMillis)
      .mapMaterializedValue(_ => NotUsed)

    override def errors: Source[Exception, NotUsed] = Source.empty
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
    )
  )

  door(bedRoom -> hallway)
  door(hallway -> external).withProperties(
    time_now()
  )

  val build = h.build()
  println(build)


  while (true) {
    println(build.floors.head.rooms.head.gateways.find(_.rooms._2 == external.build()).head.properties.head.value)
    Thread.sleep(1000)
  }
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