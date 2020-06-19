package config.factory.topology

import config.impl.HomeImpl
import model.Home
import utils.SetContainer

case class HomeFactory(override val name: String) extends DigitalTwinFactory[Home] {
  private var floorsSet = SetContainer[FloorFactory](Set(), Seq(_.name, _.level))
  private var usersSet = SetContainer[UserFactory](Set(), Seq(_.name))

  def apply(floors: FloorFactory*): this.type = this.floors(floors: _*)

  def floors(floors: FloorFactory*): this.type = {
    this.floorsSet = this.floorsSet.add(floors)
    this
  }

  def users(users: UserFactory*): this.type = {
    this.usersSet = this.usersSet.add(users)
    this
  }

  override def oneTimeBuild(): Home = HomeImpl(name, floorsSet.content.map(_.build()), propertiesSet.map(_.build()), actionsSet.map(_.build()), usersSet.content.map(_.build()))
}
