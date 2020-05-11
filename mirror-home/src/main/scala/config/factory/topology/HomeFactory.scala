package config.factory.topology

import config.impl.HomeImpl
import model.Home
import utils.SetContainer

case class HomeFactory(override val name: String) extends DigitalTwinFactory[Home] {
  private var floors = SetContainer[FloorFactory, String](Set(), Seq(_.name, _.level))
  private var users = SetContainer[UserFactory, String](Set(), Seq(_.name))

  def apply(floors: FloorFactory*): this.type = this.withFloors(floors: _*)

  def withFloors(floors: FloorFactory*): this.type = {
    this.floors = this.floors.add(floors)
    this
  }

  def withUsers(users: UserFactory*): this.type = {
    this.users = this.users.add(users)
    this
  }

  override def oneTimeBuild(): Home = HomeImpl(name, floors.content.map(_.build()), properties.map(_.build()), actions.map(_.build()), users.content.map(_.build()))
}
