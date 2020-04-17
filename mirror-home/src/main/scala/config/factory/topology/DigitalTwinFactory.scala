package config.factory.topology

import config.factory.OneTimeFactory
import config.factory.action.ActionFactory
import config.factory.property.{JsonPropertyFactory, PropertyFactory}
import model.{DigitalTwin, Property}
import utils.SetContainer

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {

  private var pContainer = SetContainer[PropertyFactory[_], String](_.name, Set())
  private var aContainer = SetContainer[ActionFactory[_], String](_.name, Set())

  def properties: Set[PropertyFactory[_]] = pContainer.content

  def actions: Set[ActionFactory[_]] = aContainer.content

  def withProperties(properties: PropertyFactory[_]*): this.type = {
    pContainer = pContainer.add(properties)
    this
  }

  def withAction(actions: ActionFactory[_]*): this.type = {
    aContainer = aContainer.add(actions)
    this
  }

  def name: String
}
