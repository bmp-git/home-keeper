package config.factory.topology

import config.impl.DoorImpl
import model.Door
import utils.Lazy

case class DoorFactory(name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Door] {
  override protected def oneTimeBuild(): Door = DoorImpl(name, new Lazy((rooms._1.build(), rooms._2.build())),
    properties.map(_.build()), actions.map(_.build()))
}
