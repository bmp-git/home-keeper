package config.factory.property

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import config.factory.ble.BleBeaconFactory
import model.Units.{BrokerAddress, MacAddress}
import model.ble.Formats._
import model.ble.{BeaconData, RawBeaconData}
import sources.MqttSource
import spray.json.DefaultJsonProtocol._

import scala.util.{Success, Try}

object MqttPropertyFactory {
  def payloads(name: String, brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): PropertyFactory[String] = {
    def sourceFactory(): Source[Try[String], _] = MqttSource.payloads(brokerAddress, topics: _*).map(Success.apply)

    PropertyFactory(name, sourceFactory)
  }

  def ble(name: String, brokerAddress: BrokerAddress, scannerMacAddress: MacAddress, beaconsFactory: Seq[BleBeaconFactory])
         (implicit actorSystem: ActorSystem): PropertyFactory[Seq[BeaconData]] =
    BlePropertyFactory(name, MqttSource.objects[RawBeaconData](brokerAddress, s"scanner/$scannerMacAddress/ble"), beaconsFactory)
}





