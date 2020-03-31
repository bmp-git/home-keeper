package config.factory.topology

import config.factory.{OneTimeFactory, PropertyFactory}
import model.{Action, DigitalTwin}

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {
  var properties: Set[PropertyFactory[_, _]] = Set[PropertyFactory[_, _]]()
  var actions: Set[Action[_]] = Set[Action[_]]()

  def withProperties(property: PropertyFactory[_, _]*): this.type = {
    properties = properties ++ property
    this
  }

  def withAction(action: Action[_]*): this.type = {
    actions = actions ++ action
    this
  }

}
