//Room, Door, Window, External env, Home, Floor,
//Beacon, Pir, Videocamera, Perimetrali
package model

import scala.util.Try

trait Property[T] {
  def name: String

  def value: Try[T]
}

trait Action[T] {
  def name: String

  def trig(t: T): Unit //Unit or Option[Exception] or Future[Try[Done]]?
}

trait DigitalTwin { //DigitalTwin situated
  def name: String
  def properties: Set[Property[_]]
  def actions: Set[Action[_]]
}

trait User extends DigitalTwin {
  def room:Option[Room]
}

//Home topology
trait Gateway extends DigitalTwin {
  def rooms: (Room, Room)
}

trait Door extends Gateway
trait Window extends Gateway
trait Room extends DigitalTwin {
  def gateways: Set[Gateway]
}
trait Floor extends DigitalTwin {
  def rooms: Set[Room]
}
trait Home extends DigitalTwin {
  def floors: Set[Floor]
}

