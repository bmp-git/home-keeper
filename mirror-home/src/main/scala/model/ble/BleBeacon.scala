package model.ble

import model.Units.MacAddress
import model.User
import spire.math.ULong
import utils.Crypto

case class BleBeacon(mac: MacAddress, key: String, attachedTo: User) {
  var counter: ULong = ULong.fromInt(0) //TODO: restore from file

  def validate(advData: String): Boolean = {
    println(advData)
    if (advData.length != 62) {
      false
    } else {
      val hash = advData.slice(26, 42)
      val c = advData.slice(42, 58)
      val ret = Crypto.BLEBeacon.verify(counter, c, key, hash)
      if (ret) {
        counter = Crypto.parseULong(c).get //TODO: save it to file
      }
      ret
    }
  }
}
