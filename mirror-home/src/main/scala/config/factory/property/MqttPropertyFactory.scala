package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import config.factory.ble.BleBeaconFactory
import model.Units.{BrokerAddress, MacAddress}
import model.ble.Formats._
import model.ble.{BeaconData, RawBeaconData}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json.DefaultJsonProtocol._
import spray.json._
import utils.RichMap._

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

    def objectExtractor[T: JsonFormat]: Flow[String, Try[T], NotUsed] =
      Flow[String].map(payload => Try(payload.parseJson.convertTo[T]))

    def payloads(brokerAddress: BrokerAddress, topics: String*)
                (implicit actorSystem: ActorSystem): Source[String, Future[Done]] =
      messages(brokerAddress, topics: _*)(actorSystem).via(payloadExtractor)

    def objects[T: JsonFormat](brokerAddress: BrokerAddress, topics: String*)
                              (implicit actorSystem: ActorSystem): Source[Try[T], Future[Done]] =
      payloads(brokerAddress, topics: _*).via(objectExtractor[T])
  }

  def payloads(name: String, brokerAddress: BrokerAddress, topics: String*)
              (implicit actorSystem: ActorSystem): PropertyFactory[String] = {
    def sourceFactory() = Flows.messages(brokerAddress, topics: _*)
      .via(Flows.payloadExtractor).map(Success.apply)
    PropertyFactory(name, sourceFactory)
  }

  def bleBeacon(name: String, brokerAddress: BrokerAddress, scannerMacAddress: MacAddress, beaconsFactory: Seq[BleBeaconFactory])
               (implicit actorSystem: ActorSystem): PropertyFactory[Seq[BeaconData]] = {

    def bleTopic(mac: MacAddress) = s"scanner/$mac/ble"

    def sourceFactory(): scaladsl.Source[Try[Seq[BeaconData]], _] = {
      var container = Map[String, BeaconData]()
      val beacons = beaconsFactory.map(_.build())
      Flows.objects[RawBeaconData](brokerAddress, bleTopic(scannerMacAddress)).map({
        case Success(receivedRaw) =>
          beacons.find(_.mac == receivedRaw.addr) match {
            case Some(beacon) if beacon.validate(receivedRaw.advData) =>
              val beaconData = BeaconData(beacon.attachedTo, DateTime.now, receivedRaw.rssi)
              container = container.addOrUpdate(beaconData.user.name -> beaconData)
            case _ => //nothing
          }
          Success(container.toValueSeq)
        case Failure(ex) => Failure(ex)
      })
    }

    PropertyFactory(name, sourceFactory)
  }

}





