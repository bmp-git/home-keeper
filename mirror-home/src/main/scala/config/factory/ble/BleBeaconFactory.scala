package config.factory.ble


import config.ConfigDsl
import config.factory.OneTimeFactory
import config.factory.topology.UserFactory
import model.Units.MacAddress
import model.ble.BleBeacon
import spire.math.ULong

object BleBeaconFactory {
  def apply(mac: MacAddress, key: String, user: UserFactory, fireRate: Double, fireRateError: Double): BleBeaconFactory
  = new BleBeaconFactory(mac, key, user, Some((fireRate, fireRateError)))
}

case class BleBeaconFactory(mac: MacAddress, key: String, user: UserFactory, fireRate: Option[(Double, Double)] = None) extends OneTimeFactory[BleBeacon] {
  override protected def oneTimeBuild(): BleBeacon = {
    val storagePath: String = s"${ConfigDsl.RESOURCE_FOLDER}/ble_beacon_${mac}_counter"
    val storagePathTime: String = s"${ConfigDsl.RESOURCE_FOLDER}/ble_beacon_${mac}_counter"
    val initialCounter: ULong = utils.File.read(storagePath).map(ULong.apply).getOrElse(ULong(0))
    val initialCounterTime: Long = utils.File.read(storagePathTime).map[Long](_.toLong).getOrElse(0)
    val counterUpdater: (ULong, Long) => Any = (c: ULong, t: Long) => {
      utils.File.write(storagePath, c.toString)
      utils.File.write(storagePathTime, t.toString)
    }
    BleBeacon(mac, key, user.build(), initialCounter, initialCounterTime, counterUpdater, fireRate)
  }
}
