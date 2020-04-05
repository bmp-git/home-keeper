package config.factory.topology

import config.impl.UserImpl
import model.User

case class UserFactory(userName: String) extends DigitalTwinFactory[User] {
  override def name: String = userName

  override protected def oneTimeBuild(): User = UserImpl(userName, properties.map(_.build()), actions.map(_.build()))
}
