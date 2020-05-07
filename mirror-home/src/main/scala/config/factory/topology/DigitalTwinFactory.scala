package config.factory.topology

import config.factory.OneTimeFactory
import config.factory.action.ActionFactory
import config.factory.property.PropertyFactory
import model.DigitalTwin
import utils.SetContainer

trait DigitalTwinFactory[T <: DigitalTwin] extends OneTimeFactory[T] {

  private var pContainer = SetContainer[PropertyFactory, String](Set(), Seq(_.name))
  private var aContainer = SetContainer[ActionFactory, String](Set(), Seq(_.name))

  def properties: Set[PropertyFactory] = pContainer.content

  def actions: Set[ActionFactory] = aContainer.content

  def withProperties(properties: PropertyFactory*): this.type = {
    pContainer = pContainer.add(properties)
    this
  }

  def withAction(actions: ActionFactory*): this.type = {
    aContainer = aContainer.add(actions)
    this
  }

  def name: String
}
