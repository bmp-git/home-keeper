package config

import model.Property

object Config {

  type BrokerAddress = String

  implicit val brokerAddress:BrokerAddress = "192.168.1.1"



  mqtt_bool("is_open", "ciao/ciao", "ON", "OFF")


  trait StaticPropertyFactory[O]  {
    def name:String
    def output: Iterable[O]
    def errors: Iterable[Exception]
    def outs:Iterable[Either[O,Exception]] = ??? //merge output and errors
  }
  trait DynamicPropertyFactory[I,O] extends StaticPropertyFactory[O]{
    def map(i:I):O
    def input: Iterable[I]
    def output: Iterable[O] = input.map(map)
  }


  case class TimePropertyFactory() extends StaticPropertyFactory[Int] {
    override def name: String = "datetime"

    override def output: Iterable[Int] = new Iterable[Int] {
      override def iterator: Iterator[Int] = new Iterator[Int] {
        override def hasNext: Boolean = true

        override def next(): Int = {
          //wait 1 sec
          //time.now
          ???
        }
      }
    }

    override def errors: Iterable[Exception] = ???
  }
  case class TimeProperty() extends Property[Int] {
    override def name: String = ???
    override def value: Int = ???
  }

  def mqtt_bool(name:String, topic: String, truePayload: String, falsePayload:String)(implicit brokerAddress: BrokerAddress)
  :MqttBooleanPropertyFactory = new MqttBooleanPropertyFactory(name, brokerAddress, topic) {
    override def map(i: String): Boolean = i match {
      case `truePayload` => true
      case `falsePayload` => false
    }
  }



  abstract class MqttBooleanPropertyFactory(override val name:String, brokerAddress:BrokerAddress, topic:String)
    extends DynamicPropertyFactory[String,Boolean] {
    override def input: Iterable[String] = ???

    override def errors: Iterable[Exception] = ???
  }


  object TopologyFactory {

  }

}
