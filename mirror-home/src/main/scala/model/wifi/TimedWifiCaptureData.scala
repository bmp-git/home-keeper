package model.wifi

import akka.http.scaladsl.model.DateTime
import model.Units.{MacAddress, Db}

case class TimedWifiCaptureData(mac: MacAddress, rssi: Db, lastSeen: DateTime)
