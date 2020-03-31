package config

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.Source
import config.factory.topology._
import config.factory.{topology, _}
import model.Property
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ConfigDsl {

  type BrokerAddress = String

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

  //Properties
  def time_now(): PropertyFactory[Long] = PropertyFactory.safe("time",
    Source.tick(0.second, 1.second, None).map(_ => System.currentTimeMillis))

  def tag(name: String, value: String): PropertyFactory[String] = PropertyFactory.static(name, value)

  implicit val system: ActorSystem = ActorSystem()

  def mqtt_bool(name: String, stateTopic: String, truePayload: String, falsePayload: String)
               (implicit brokerAddress: BrokerAddress): PropertyFactory[Boolean] =
    mqtt_payload(name, stateTopic).valueMap(_.payload.utf8String).flatMap {
      case `truePayload` => Success(true)
      case `falsePayload` => Success(false)
      case v => Failure(new Exception(s"$name MqttBool property: $v match failure"))
    }

  private def mqtt_payload(name: String, topic: String)(implicit brokerAddress: BrokerAddress): PropertyFactory[MqttMessage] = {
    val connectionSettings = MqttConnectionSettings(
      s"tcp://$brokerAddress",
      s"mirror-home-property-$name",
      new MemoryPersistence
    )
    val mqttSource: Source[MqttMessage, Future[Done]] =
      MqttSource.atMostOnce(
        connectionSettings,
        MqttSubscriptions(Map(topic -> MqttQoS.AtLeastOnce)),
        bufferSize = 1
      )
    PropertyFactory(name, mqttSource.map(Success.apply))
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
    //time_now()
  )

  val p1 = time_now().build()
  val p2 = tag("Time", "now: ").build()
  implicit val brokerAddress: BrokerAddress = "192.168.1.10:1883"
  val p3 = mqtt_bool("PC", "stat/shelly25_1/POWER1", "ON", "OFF").build()
  val p4 = mqtt_bool("Letto", "stat/shelly25_1/POWER2", "ON", "OFF").build()
  val build = h.build()
  println(build)

  def printProperty(p: Property[Boolean]): Unit = {
    val state = p.value match {
      case Failure(exception) => exception match {
        case _: NoSuchElementException => "Unknown"
        case ex => ex.getMessage
      }
      case Success(true) => "On"
      case Success(false) => "Off"
    }
    println(s"${p.name}: $state")
  }

  while (true) {
    printProperty(p3)
    printProperty(p4)
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