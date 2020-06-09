package sources

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import model.BrokerConfig
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json._
import utils.{IdDispatcher}

import scala.concurrent.Future
import scala.util.{Random, Try}

object MqttSource {
  val idDispatcher: IdDispatcher = IdDispatcher(0)

  def messages(brokerConfig: BrokerConfig, topics: String*) //TODO: what if crash?
              (implicit actorSystem: ActorSystem): Source[MqttMessage, Future[Done]] = {
    var connectionSettings = MqttConnectionSettings(
      s"tcp://${brokerConfig.address}",
      s"mirror-home-property-" + idDispatcher.next, //TODO:random?
      new MemoryPersistence //TODO: need an explanation
    ).withAutomaticReconnect(true)
    connectionSettings = brokerConfig.auth match {
      case Some((user, pass)) => connectionSettings.withAuth(user, pass)
      case None => connectionSettings
    }

    akka.stream.alpakka.mqtt.scaladsl.MqttSource.atMostOnce(
      connectionSettings,
      MqttSubscriptions(Map(topics.map(_ -> MqttQoS.AtLeastOnce): _*)),
      bufferSize = 1
    )
  }

  def payloads(brokerConfig: BrokerConfig, topics: String*)
              (implicit actorSystem: ActorSystem): Source[String, Future[Done]] =
    messages(brokerConfig, topics: _*)(actorSystem).via(payloadExtractor)

  def objects[T: JsonFormat](brokerConfig: BrokerConfig, topics: String*)
                            (implicit actorSystem: ActorSystem): Source[Try[T], Future[Done]] =
    payloads(brokerConfig, topics: _*).via(objectExtractor[T])


  private def payloadExtractor: Flow[MqttMessage, String, NotUsed] =
    Flow[MqttMessage].map(_.payload.utf8String)

  private def objectExtractor[T: JsonFormat]: Flow[String, Try[T], NotUsed] =
    Flow[String].map(payload => Try(payload.parseJson.convertTo[T]))
}
