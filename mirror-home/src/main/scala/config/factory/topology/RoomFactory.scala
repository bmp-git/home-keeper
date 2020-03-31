package config.factory.topology

import config.impl.RoomImpl
import model.{Gateway, Room}
import utils.Lazy

case class RoomFactory(name: String) extends DigitalTwinFactory[Room] {
  private var gateways = Seq[GatewayFactory[_ <: Gateway]]()

  def withGateways[T <: Gateway](gateway: GatewayFactory[T]*): this.type = {
    gateways = gateways ++ gateway
    this
  }

  override def oneTimeBuild(): Room = RoomImpl(name, new Lazy(gateways.map(_.build()).toSet),
    properties.map(_.build()), actions)
}
