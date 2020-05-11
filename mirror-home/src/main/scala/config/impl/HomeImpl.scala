package config.impl

import model._

case class HomeImpl(override val name: String,
                    override val floors: Set[Floor],
                    override val properties: Set[Property],
                    override val actions: Set[Action],
                    override val users: Set[User]) extends Home
