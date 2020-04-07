package sources

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import model.Units.BrokerAddress
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json._

import scala.concurrent.Future
import scala.util.Try

object MqttSource {
  def messages(brokerAddress: BrokerAddress, topics: String*) //TODO: what if crash?
              (implicit actorSystem: ActorSystem): Source[MqttMessage, Future[Done]] = {
    val connectionSettings = MqttConnectionSettings(
      s"tcp://$brokerAddress",
      s"mirror-home-property-" + System.currentTimeMillis(), //TODO:random?
      new MemoryPersistence //TODO: need an explanation
    )

    akka.stream.alpakka.mqtt.scaladsl.MqttSource.atMostOnce(
      connectionSettings,
      MqttSubscriptions(Map(topics.map(_ -> MqttQoS.AtLeastOnce): _*)),
      bufferSize = 1
    )
  }

  def payloads(brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): Source[String, Future[Done]] =
    messages(brokerAddress, topics: _*)(actorSystem).via(payloadExtractor)

  def objects[T: JsonFormat](brokerAddress: BrokerAddress, topics: String*)
                            (implicit actorSystem: ActorSystem): Source[Try[T], Future[Done]] =
    payloads(brokerAddress, topics: _*).via(objectExtractor[T])


  private def payloadExtractor: Flow[MqttMessage, String, NotUsed] =
    Flow[MqttMessage].map(_.payload.utf8String)

  private def objectExtractor[T: JsonFormat]: Flow[String, Try[T], NotUsed] =
    Flow[String].map(payload => Try(payload.parseJson.convertTo[T]))
}
