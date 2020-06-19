package config.factory.topology

import config.impl.WindowImpl
import model.Window
import utils.Lazy

case class WindowFactory(override val name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Window] {
  rooms._1.gateways(this)
  rooms._2.gateways(this)

  override protected def oneTimeBuild(): Window = WindowImpl(name, new Lazy((rooms._1.build(), rooms._2.build())),
    propertiesSet.map(_.build()), actionsSet.map(_.build()))
}
