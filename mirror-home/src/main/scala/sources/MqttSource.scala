package sources

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import model.BrokerConfig
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json._
import utils.IdDispatcher

import scala.concurrent.Future
import scala.util.Try

object MqttSource {
  val idDispatcher: IdDispatcher = IdDispatcher(0)

  def settings(brokerConfig: BrokerConfig): MqttConnectionSettings = {
    val connectionSettings = MqttConnectionSettings(
      s"tcp://${brokerConfig.address}",
      s"mirror-home-property-" + idDispatcher.next,
      new MemoryPersistence
    ).withAutomaticReconnect(true)
    brokerConfig.auth match {
      case Some((user, pass)) => connectionSettings.withAuth(user, pass)
      case None => connectionSettings
    }
  }

  //TODO: need a reconnection mechanism without closing the stream
  def messages(brokerConfig: BrokerConfig, topics: String*)
              (implicit actorSystem: ActorSystem): Source[MqttMessage, Future[Done]] = {
    akka.stream.alpakka.mqtt.scaladsl.MqttSource.atMostOnce(
      settings(brokerConfig),
      MqttSubscriptions(Map(topics.map(_ -> MqttQoS.AtLeastOnce): _*)),
      bufferSize = 1
    )
  }

  def payloads(brokerConfig: BrokerConfig, topics: String*)
              (implicit actorSystem: ActorSystem): Source[String, Future[Done]] =
    messages(brokerConfig, topics: _*)(actorSystem).via(payloadExtractor)

  def topicsAndPayloads(brokerConfig: BrokerConfig, topics: String*)
                       (implicit actorSystem: ActorSystem): Source[(String, String), Future[Done]] =
    messages(brokerConfig, topics: _*)(actorSystem).via(topicAndPayloadExtractor)

  def objects[T: JsonFormat](brokerConfig: BrokerConfig, topics: String*)
                            (implicit actorSystem: ActorSystem): Source[Try[T], Future[Done]] =
    payloads(brokerConfig, topics: _*).via(objectExtractor[T])

  def topicsAndObjects[T: JsonFormat](brokerConfig: BrokerConfig, topics: String*)
                                    (implicit actorSystem: ActorSystem): Source[Try[(String, T)], Future[Done]] =
    messages(brokerConfig, topics: _*).via(topicAndObjectExtractor[T])

  private def payloadExtractor: Flow[MqttMessage, String, NotUsed] =
    Flow[MqttMessage].map(_.payload.utf8String)

  private def topicAndPayloadExtractor: Flow[MqttMessage, (String, String), NotUsed] =
    Flow[MqttMessage].map(m => (m.topic, m.payload.utf8String))

  private def objectExtractor[T: JsonFormat]: Flow[String, Try[T], NotUsed] =
    Flow[String].map(payload => Try(payload.parseJson.convertTo[T]))

  private def topicAndObjectExtractor[T: JsonFormat]: Flow[MqttMessage, Try[(String, T)], NotUsed] =
    Flow[MqttMessage].map(message => Try(message.payload.utf8String.parseJson.convertTo[T]).map(o => (message.topic, o)))
}
