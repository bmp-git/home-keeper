package config.factory.topology

import config.impl.RoomImpl
import model.{Gateway, Room}
import utils.{Lazy, SetContainer}

case class RoomFactory(override val name: String) extends DigitalTwinFactory[Room] {
  private var gateways = SetContainer[GatewayFactory[_ <: Gateway], String](Set(), Seq(_.name))

  def apply[T <: Gateway](gateways: GatewayFactory[T]*): this.type = withGateways(gateways: _*)

  def withGateways[T <: Gateway](gateways: GatewayFactory[T]*): this.type = {
    this.gateways = this.gateways.add(gateways)
    this
  }

  override def oneTimeBuild(): Room = RoomImpl(name, new Lazy(gateways.content.map(_.build())),
    properties.map(_.build()), actions.map(_.build()))
}
