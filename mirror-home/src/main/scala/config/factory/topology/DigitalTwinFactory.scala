package config.factory.topology

import model.{Action, DigitalTwin, Property}

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {
  var properties: Set[Property[_]] = Set[Property[_]]()
  var actions: Set[Action[_]] = Set[Action[_]]()

  def withProperties(property: Property[_]*): this.type = {
    properties = properties ++ property
    this
  }

  def withAction(action: Action[_]*): this.type = {
    actions = actions ++ action
    this
  }

}
