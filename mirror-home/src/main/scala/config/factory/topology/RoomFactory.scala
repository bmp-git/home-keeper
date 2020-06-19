package config.factory.topology

import config.impl.RoomImpl
import model.{Gateway, Room}
import utils.{Lazy, SetContainer}

case class RoomFactory(override val name: String) extends DigitalTwinFactory[Room] {
  private var gatewaysSet = SetContainer[GatewayFactory[_ <: Gateway]](Set(), Seq(_.name))

  def apply[T <: Gateway](gateways: GatewayFactory[T]*): this.type = this.gateways(gateways: _*)

  def gateways[T <: Gateway](gateways: GatewayFactory[T]*): this.type = {
    this.gatewaysSet = this.gatewaysSet.add(gateways)
    this
  }

  override def oneTimeBuild(): Room = RoomImpl(name, new Lazy(gatewaysSet.content.map(_.build())),
    propertiesSet.map(_.build()), actionsSet.map(_.build()))
}
