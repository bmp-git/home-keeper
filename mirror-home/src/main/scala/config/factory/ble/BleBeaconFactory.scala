package config.factory.ble


import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import config.ConfigDsl
import config.factory.OneTimeFactory
import config.factory.topology.UserFactory
import model.Units.MacAddress
import model.ble.BleBeacon
import spire.math.ULong

import scala.util.{Failure, Success, Try}

case class BleBeaconFactory(mac: MacAddress, key: String, user: UserFactory) extends OneTimeFactory[BleBeacon] {
  override protected def oneTimeBuild(): BleBeacon = {
    val storagePath = s"${ConfigDsl.RESOURCE_FOLDER}\\ble_beacon_${mac}_counter"
    val initialCounter = Try(scala.io.Source.fromFile(storagePath)) match {
      case Failure(_) => ULong.fromInt(0)
      case Success(source) =>
        val c = Try(source.mkString) match {
          case Failure(_) => ULong.fromInt(0)
          case Success(content) => ULong(content)
        }
        source.close()
        c
    }

    def counterUpdater(newCounter: ULong): Unit =
      Try(Files.write(Paths.get(storagePath), newCounter.toString.getBytes(StandardCharsets.UTF_8)))

    BleBeacon(mac, key, user.build(), initialCounter, counterUpdater)
  }
}
