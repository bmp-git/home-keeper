package config.impl

import model.{Action, Gateway, Property, Room}
import utils.Lazy

case class RoomImpl(override val name: String,
                    gatewaysGetter: Lazy[Set[Gateway]],
                    override val properties: Set[Property],
                    override val actions: Set[Action]
                   ) extends Room {
  lazy val _gateways: Set[Gateway] = gatewaysGetter.value
  override def gateways: Set[Gateway] = _gateways
  override def toString: String = s"Room($name, ${gateways.map(_.name)}, $properties, $actions)"
}
