package config.factory.topology

import config.impl.UserImpl
import model.User

case class UserFactory(firstname: String, surname:String) extends DigitalTwinFactory[User] {
  override def name: String = s"$firstname$surname".toLowerCase

  override protected def oneTimeBuild(): User = UserImpl(firstname, surname , propertiesSet.map(_.build()), actionsSet.map(_.build()))
}
