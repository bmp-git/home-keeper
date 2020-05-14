package config.factory.ble


import config.ConfigDsl
import config.factory.OneTimeFactory
import config.factory.topology.UserFactory
import model.Units.MacAddress
import model.ble.BleBeacon
import spire.math.ULong

case class BleBeaconFactory(mac: MacAddress, key: String, user: UserFactory) extends OneTimeFactory[BleBeacon] {
  override protected def oneTimeBuild(): BleBeacon = {
    val storagePath = s"${ConfigDsl.RESOURCE_FOLDER}/ble_beacon_${mac}_counter"
    val initialCounter = utils.File.read(storagePath).map(ULong.apply).getOrElse(ULong(0))
    val counterUpdater = (c: ULong) => utils.File.write(storagePath, c.toString)
    BleBeacon(mac, key, user.build(), initialCounter, counterUpdater)
  }
}
