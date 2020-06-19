package sinks

import akka.Done
import akka.stream.alpakka.mqtt.{MqttMessage, MqttQoS}
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import model.BrokerConfig
import sources.MqttSource

import scala.concurrent.Future

object MqttSink {

  def messages(brokerConfig: BrokerConfig): Sink[MqttMessage, Future[Done]] = {
    akka.stream.alpakka.mqtt.scaladsl.MqttSink(MqttSource.settings(brokerConfig), MqttQoS.AtLeastOnce)
  }

  def fixedTopic(topic: String, brokerConfig: BrokerConfig): Sink[String, Future[Done]] =
    Flow[String].map(payload => MqttMessage(topic, ByteString(payload))).toMat(messages(brokerConfig))
}
