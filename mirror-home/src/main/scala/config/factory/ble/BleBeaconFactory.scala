package config.factory.ble


import config.ConfigDsl
import config.factory.OneTimeFactory
import config.factory.topology.UserFactory
import model.Units.MacAddress
import model.User
import model.ble.{BleBeacon, BleBeaconImpl}
import spire.math.ULong
import utils.Crypto

object BleBeaconFactory {
  def apply(mac: MacAddress, key: String, user: UserFactory, fireRate: Double, fireRateError: Double): BleBeaconFactory
  = BleBeaconFactory(mac, key, user, Some((fireRate, fireRateError)))

  def dummy(_mac: MacAddress, _key: String, _user: UserFactory): OneTimeFactory[BleBeacon] = () => new BleBeacon {
    override def attachedTo: User = _user.build()

    override def key: String = _key

    override def mac: MacAddress = _mac

    override def validate(advData: String): Boolean = {
      if (advData.length != 62) {
        false
      } else {
        val hash = advData.slice(26, 42)
        val c = advData.slice(42, 58)
        Crypto.BLEBeacon.verifyDummy(c, key, hash) match {
          case Some(_) => true
          case _ => false
        }
      }
    }
  }
}

case class BleBeaconFactory(mac: MacAddress, key: String, user: UserFactory, fireRate: Option[(Double, Double)] = None) extends OneTimeFactory[BleBeacon] {
  override protected def oneTimeBuild(): BleBeacon = {
    val storagePath: String = s"${ConfigDsl.RESOURCE_FOLDER}/ble_beacon_${mac}_counter"
    val storagePathTime: String = s"${ConfigDsl.RESOURCE_FOLDER}/ble_beacon_${mac}_counter_time"
    val initialCounter: ULong = utils.File.read(storagePath).map(ULong.apply).getOrElse(ULong(0))
    val initialCounterTime: Long = utils.File.read(storagePathTime).map[Long](_.toLong).getOrElse(0)
    val counterUpdater: (ULong, Long) => Any = (c: ULong, t: Long) => {
      utils.File.write(storagePath, c.toString)
      utils.File.write(storagePathTime, t.toString)
    }
    BleBeaconImpl(mac, key, user.build(), initialCounter, initialCounterTime, counterUpdater, fireRate)
  }
}
