package config.factory.ble

import config.factory.OneTimeFactory
import config.factory.topology.UserFactory
import model.Units.MacAddress
import model.ble
import model.ble.BleBeacon
import spire.math.ULong

case class BleBeaconFactory(mac: MacAddress, key: String, user: UserFactory) extends OneTimeFactory[BleBeacon] {
  override protected def oneTimeBuild(): BleBeacon = ble.BleBeacon(mac, key, user.build(), ULong.fromInt(0), _ => ())
  //TODO: read initialCounter from file and update the file
}
