package config.factory.topology

import model.{Door, Gateway, Window}
import utils.Lazy

trait GatewayFactory[T <: Gateway] extends DigitalTwinFactory[T]

case class DoorFactory(name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Door] {
  override protected def oneTimeBuild(): Door = DoorImpl(name, new Lazy((rooms._1.build(),rooms._2.build())), properties, actions)
}

case class WindowFactory(name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Window] {
  override protected def oneTimeBuild(): Window = WindowImpl(name,  new Lazy((rooms._1.build(),rooms._2.build())), properties, actions)
}


