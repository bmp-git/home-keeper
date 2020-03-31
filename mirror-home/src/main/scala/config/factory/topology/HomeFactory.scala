package config.factory.topology

import config.impl.HomeImpl
import model.Home

case class HomeFactory(name: String) extends DigitalTwinFactory[Home] {
  private var floors = Seq[FloorFactory]()

  def apply(floors: FloorFactory*): this.type = {
    this.withFloors(floors:_*)
    this
  }

  def withFloors(floors: FloorFactory*): this.type = {
    this.floors = this.floors ++ floors
    this
  }

  override def oneTimeBuild(): Home = HomeImpl(name, floors.map(_.build()).toSet, properties, actions)
}
