package config.impl

import model.{Action, Property, User}

case class UserImpl(override val firstname: String,
                    override val surname: String,
                    override val properties: Set[Property],
                    override val actions: Set[Action]
                   ) extends User
