package config.impl

import model.{Action, Floor, Home, Property}

case class HomeImpl(override val name: String,
                    override val floors: Set[Floor],
                    override val properties: Set[Property[_]],
                    override val actions: Set[Action[_]]) extends Home
