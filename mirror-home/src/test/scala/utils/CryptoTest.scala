package utils

import org.scalatest.FunSuite

class CryptoTest extends FunSuite {
  test("BLE Beacon hash tests 1") {
    val key = "SimpleBLEBroadca"
    val counter = "0000000000003cf1"
    assert(Crypto.BLEBeacon.verify(counter, key, "a620ebc55c37763a"))
  }

  test("BLE Beacon hash tests 2") {
    val key = "SimpleBLEBroadca"
    val counter = "0000000000003cec"
    assert(Crypto.BLEBeacon.verify(counter, key, "f4266cd2cb3ae77b"))
  }

  test("BLE Beacon hash tests 3") {
    val key = "SimpleBLEBroadca"
    val counter = "0000000000003ced"
    assert(Crypto.BLEBeacon.verify(counter, key, "cc9a8929f2f34e8d"))
  }

  test("BLE Beacon hash tests 4") {
    val key = "SimpleBLEBroadce"
    val counter = "0000000000003ced"
    assert(!Crypto.BLEBeacon.verify(counter, key, "cc9a8929f2f34e8d"))
  }

  test("BLE Beacon hash tests 5") {
    val key = "SimpleBLEBroadca"
    val counter = "k0000000000003ced"
    assert(!Crypto.BLEBeacon.verify(counter, key, "cc9a8929f2f34e8d"))
  }

  test("BLE Beacon hash tests 6") {
    val key = "SimpleBLEBroadca"
    val counter = "00000000003ced"
    assert(!Crypto.BLEBeacon.verify(counter, key, "cc9a8929f2f34e8d"))
  }

  test("BLE Beacon hash tests 7") {
    val key = "SimpleBLEBroadca"
    val counter = "0000000000003ced"
    assert(!Crypto.BLEBeacon.verify(counter, key, "9a8929f2f34e8d"))
  }
}
