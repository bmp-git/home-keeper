//Room, Door, Window, External env, Home, Floor,
//Beacon, Pir, Videocamera, Perimetrali
trait Property {
  def name: String // ??
}

trait Action {

}

trait Entity {
  def name: String
  def properties: Set[Property]
  def actions: Set[Action]
}

trait DigitalTwin { //DigitalTwin situated
  def name: String // ??
  def properties: Set[Property]
  def actions: Set[Action]
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
trait DummyDoor extends Door
trait External extends Room

class ActorGateway(override val name:String, override val rooms: (Room, Room)) extends Gateway {
  override def properties: Set[Property] = ???

  override def actions: Set[Action] = ???
}