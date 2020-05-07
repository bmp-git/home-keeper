package config.impl

import model.{Action, Floor, Property, Room}

case class FloorImpl(override val name: String,
                     override val rooms: Set[Room],
                     override val properties: Set[Property],
                     override val actions: Set[Action]) extends Floor
