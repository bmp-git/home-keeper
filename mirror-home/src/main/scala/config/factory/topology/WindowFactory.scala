package config.factory.topology

import config.impl.WindowImpl
import model.Window
import utils.Lazy

case class WindowFactory(name: String, rooms: (RoomFactory, RoomFactory)) extends GatewayFactory[Window] {
  override protected def oneTimeBuild(): Window = WindowImpl(name, new Lazy((rooms._1.build(), rooms._2.build())),
    properties.map(_.build()), actions)
}
