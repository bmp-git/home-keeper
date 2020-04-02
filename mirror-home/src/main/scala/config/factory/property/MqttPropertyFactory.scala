package config.factory.property

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.Source
import config.ConfigDsl.BrokerAddress
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.util.Success
import spray.json.DefaultJsonProtocol._

object MqttPropertyFactory {

  object Flows {
    def messages(name: String, brokerAddress: BrokerAddress, topics: String*) //TODO: what if crash?
                (implicit actorSystem: ActorSystem): Source[MqttMessage, Future[Done]] = {
      val connectionSettings = MqttConnectionSettings(
        s"tcp://$brokerAddress",
        s"mirror-home-property-$name",
        new MemoryPersistence //TODO: need an explanation
      )

      MqttSource.atMostOnce(
        connectionSettings,
        MqttSubscriptions(Map(topics.map(_ -> MqttQoS.AtLeastOnce): _*)),
        bufferSize = 1
      )
    }
  }

  /*def messages(name: String, brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): PropertyFactory[MqttMessage] =
    PropertyFactory(name, Flows.messages(name, brokerAddress, topics: _*).map(Success.apply))*/

  def payloads(name: String, brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): PropertyFactory[String] =
    PropertyFactory(name, Flows.messages(name, brokerAddress, topics: _*).map(_.payload.utf8String).map(Success.apply))

}





