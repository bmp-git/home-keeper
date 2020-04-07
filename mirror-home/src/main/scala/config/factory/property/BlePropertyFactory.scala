package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.{Flow, Source}
import config.factory.ble.BleBeaconFactory
import model.ble.Formats._
import model.ble.{BeaconData, RawBeaconData}
import spray.json.DefaultJsonProtocol._
import utils.RichMap._

import scala.util.{Failure, Success, Try}

object BlePropertyFactory {

  private def rawDataAnalyzer(beaconsFactory: Seq[BleBeaconFactory]): Flow[Try[RawBeaconData], Try[Seq[BeaconData]], _] = {
    var container = Map[String, BeaconData]()
    val beacons = beaconsFactory.map(_.build())
    Flow[Try[RawBeaconData]].map({
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

  def apply(name: String, rawDataSource: Source[Try[RawBeaconData], _], beaconsFactory: Seq[BleBeaconFactory])
               (implicit actorSystem: ActorSystem): PropertyFactory[Seq[BeaconData]] =
    PropertyFactory(name, () => rawDataSource.via(rawDataAnalyzer(beaconsFactory)))
}
