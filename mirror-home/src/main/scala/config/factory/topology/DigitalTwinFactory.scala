package config.factory.topology

import config.factory.OneTimeFactory
import config.factory.action.ActionFactory
import config.factory.property.PropertyFactory
import model.DigitalTwin

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {
  var properties: Set[PropertyFactory[_]] = Set[PropertyFactory[_]]()
  var actions: Set[ActionFactory[_]] = Set[ActionFactory[_]]()

  def withProperties(property: PropertyFactory[_]*): this.type = {
    properties = properties ++ property
    this
  }

  def withAction(action: ActionFactory[_]*): this.type = {
    actions = actions ++ action
    this
  }

}
