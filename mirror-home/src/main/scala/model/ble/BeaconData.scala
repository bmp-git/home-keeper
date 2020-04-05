package model.ble

import akka.http.scaladsl.model.DateTime
import model.Units.Db
import model.User

case class BeaconData(user: User, lastSeen: DateTime, rssi: Db)
