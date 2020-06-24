package model.ble

import model.Units.MacAddress
import model.User
import spire.math.ULong
import utils.Crypto
trait BleBeacon {
  def attachedTo: User
  def key: String
  def mac: MacAddress
  def validate(advData: String): Boolean
}

case class BleBeaconImpl(mac: MacAddress, key: String, attachedTo: User, initialCounter: ULong, initialLastCounterTime: Long,
                     registerNewCounter: (ULong, Long) => Any, fireRate: Option[(Double, Double)]) extends BleBeacon {
  var counter: ULong = initialCounter
  var lastCounterTime: Long = initialLastCounterTime

  //Need the fire rate of the beacon, with the % of error: ex beacon fires every
  //0.5 seconds with an error of 10% ==> after 100 seconds without receiving any
  //signal the expected counter will be: old + (100 / 0.5) = old + 200
  //so the acceptable range should be: [old + 180, old + 220] (take into account the error)
  def validate(advData: String): Boolean = {
    if (advData.length != 62) {
      false
    } else {
      val hash = advData.slice(26, 42)
      val c = advData.slice(42, 58)
      Crypto.BLEBeacon.verify(counter, c, key, hash) match {
        case Some(value) =>
          fireRate match {
            case Some((rate, error)) =>
              val dt = (System.currentTimeMillis() - lastCounterTime) / 1000.0 //delta time in second
              val signalsLost = dt / rate
              val expectedCountMin = counter + ULong.fromLong((signalsLost - (signalsLost * error)).toLong)
              val expectedCountMax = counter + ULong.fromLong((signalsLost + (signalsLost * error)).toLong)
              val skipCheck = signalsLost < 10 //not a problem, the beacon should be fast
              val firstCheck = lastCounterTime == 0
              val counterIsAcceptable = expectedCountMin <= counter && counter <= expectedCountMax
              if (skipCheck || firstCheck || counterIsAcceptable) {
                val now = System.currentTimeMillis()
                registerNewCounter(value, now)
                counter = value
                lastCounterTime = now
                true
              } else {
                false
              }
            case None =>
              //can't calculate the expected counter, counter is accepted
              val now = System.currentTimeMillis()
              registerNewCounter(value, now)
              counter = value
              lastCounterTime = now
              true
          }
        case None => false
      }

    }
  }
}
