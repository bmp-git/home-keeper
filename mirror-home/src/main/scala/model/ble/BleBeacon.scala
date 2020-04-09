package model.ble

import model.Units.MacAddress
import model.User
import spire.math.ULong
import utils.Crypto
case class BleBeacon(mac: MacAddress, key: String, attachedTo: User, initialCounter: ULong, registerNewCounter: ULong => Any) {
  var counter: ULong = initialCounter

  //TODO: keep expected counter and validate only if in a range
  //TODO: for the first packet received ignore this
  def validate(advData: String): Boolean = {

    if (advData.length != 62) {
      false
    } else {
      val hash = advData.slice(26, 42)
      val c = advData.slice(42, 58)
      val ret = Crypto.BLEBeacon.verify(counter, c, key, hash)
      if (ret) {
        counter = Crypto.parseULong(c) match {
          case Some(value) => registerNewCounter(value); counter
          case None => counter
        }
      }
      ret
    }
  }
}
