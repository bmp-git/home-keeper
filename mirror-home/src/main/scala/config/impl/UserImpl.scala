package config.impl

import model.{Action, Property, User}

case class UserImpl(override val name: String,
                    override val properties: Set[Property[_]],
                    override val actions: Set[Action[_]]
                   ) extends User
