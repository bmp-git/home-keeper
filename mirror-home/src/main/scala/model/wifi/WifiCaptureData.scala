package model.wifi

import akka.http.scaladsl.model.DateTime
import model.Units.{MacAddress, Db}

case class WifiCaptureData(mac: MacAddress, rssi: Db) {
  def timed: TimedWifiCaptureData = TimedWifiCaptureData(mac, rssi, DateTime.now)
}