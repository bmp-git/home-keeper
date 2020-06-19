package config.factory.topology

import config.impl.DoorImpl
import model.Door
import utils.Lazy

case class DoorFactory(override val name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Door] {
  rooms._1.gateways(this)
  rooms._2.gateways(this)

  override protected def oneTimeBuild(): Door = DoorImpl(name, new Lazy((rooms._1.build(), rooms._2.build())),
    propertiesSet.map(_.build()), actionsSet.map(_.build()))
}
