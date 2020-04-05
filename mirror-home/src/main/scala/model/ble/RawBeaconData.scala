package model.ble

import model.Units.{Db, MacAddress}

case class RawBeaconData(addr: MacAddress, rssi: Db, advData: String)
