object Config {

  type BrokerAddress = String

  implicit val brokerAddress:BrokerAddress = ""



  mqtt_bool("is_open", "ciao/ciao", "ON", "OFF")











  trait PropertyFactory[I,O] {
    def name:String
    def map(i:I):O
    def input: Iterable[I]
    def errors: Iterable[Exception]
  }


  def mqtt_bool(name:String, topic: String, truePayload: String, falsePayload:String)(implicit brokerAddress: BrokerAddress)
  :MqttBooleanPropertyFactory = new MqttBooleanPropertyFactory(name, brokerAddress, topic) {
    override def map(i: String): Boolean = i match {
      case `truePayload` => true
      case `falsePayload` => false
    }
  }



  abstract class MqttBooleanPropertyFactory(override val name:String, brokerAddress:BrokerAddress, topic:String) extends PropertyFactory[String,Boolean] {
    override def input: Iterable[String] = ???

    override def errors: Iterable[Exception] = ???
  }

}
