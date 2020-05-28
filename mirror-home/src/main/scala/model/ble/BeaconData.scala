package model.ble

import akka.http.scaladsl.model.DateTime
import model.Units.Db
import model.User

case class BeaconData(userName: String, lastSeen: DateTime, rssi: Db)
