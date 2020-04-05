package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import config.factory.ble.BleBeaconFactory
import model.Units.BrokerAddress
import model.ble.{BeaconData, RawBeaconData}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json.DefaultJsonProtocol._
import utils.SetContainer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object MqttPropertyFactory {

  object Flows {
    def messages(brokerAddress: BrokerAddress, topics: String*) //TODO: what if crash?
                (implicit actorSystem: ActorSystem): Source[MqttMessage, Future[Done]] = {
      val connectionSettings = MqttConnectionSettings(
        s"tcp://$brokerAddress",
        s"mirror-home-property-" + System.currentTimeMillis(), //TODO:random?
        new MemoryPersistence //TODO: need an explanation
      )

      MqttSource.atMostOnce(
        connectionSettings,
        MqttSubscriptions(Map(topics.map(_ -> MqttQoS.AtLeastOnce): _*)),
        bufferSize = 1
      )
    }

    def payloadExtractor: Flow[MqttMessage, String, NotUsed] =
      Flow[MqttMessage].map(_.payload.utf8String)

    def payloads(brokerAddress: BrokerAddress, topics: String*)
                (implicit actorSystem: ActorSystem): Source[String, Future[Done]] =
      messages(brokerAddress, topics: _*)(actorSystem).via(payloadExtractor)
  }

  def payloads(name: String, brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): PropertyFactory[String] = {
    def sourceFactory() = Flows.messages(brokerAddress, topics: _*)
      .via(Flows.payloadExtractor).map(Success.apply)
    PropertyFactory(name, sourceFactory)
  }

  def bleBeacon(name: String, brokerAddress: BrokerAddress, receiverMac: String, beaconsFactory: Seq[BleBeaconFactory])
               (implicit actorSystem: ActorSystem): PropertyFactory[Seq[BeaconData]] = {
    import spray.json._
    import DefaultJsonProtocol._
    import model.ble.Formats._

    def sourceFactory(): scaladsl.Source[Try[Seq[BeaconData]], _] = {
      var container = Map[String, BeaconData]()
      val beacons = beaconsFactory.map(_.build())
      MqttPropertyFactory.Flows.payloads(brokerAddress, s"scanner/$receiverMac/ble").map(payload => {
        Try(payload.parseJson.convertTo[RawBeaconData])
      }).map({
        case Success(raw) =>
          beacons.find(_.mac == raw.addr) match {
            case Some(beacon) if beacon.validate(raw.advData) =>
              val newData = BeaconData(beacon.attachedTo, DateTime.now, raw.rssi)
              if(container.isDefinedAt(newData.user.name)) {
                container = container.map {
                  case (key, _) if key == newData.user.name => newData.user.name -> newData
                  case x => x
                }
              } else {
                container = container + (newData.user.name -> newData)
              }
            case _ => //nothing
          }
          Success(container.toSeq.map(_._2))
        case Failure(exception) => Failure(exception)
      })
    }

    PropertyFactory(name, sourceFactory)
  }

}





