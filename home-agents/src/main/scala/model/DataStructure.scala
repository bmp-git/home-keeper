package model

case class BeaconData(user: String, last_seen: Long, rssi: Int)

case class BeaconDataSeq(seq: Seq[BeaconData])

case class TimedWifiCaptureData(mac: String, rssi: Int, lastSeen: Long)

case class TimedWifiCaptureDataSeq(seq: Seq[TimedWifiCaptureData])

case class Coordinates(latitude: Double, longitude: Double)

case class SmartphoneData(latitude: Double,
                          longitude: Double,
                          timestamp: Long,
                          accuracy: Int)

case class ReceiverStatus(online: Boolean)

sealed trait UserPosition

case object Unknown extends UserPosition

case object AtHome extends UserPosition

case object Away extends UserPosition

case class InRoom(floorName: String, roomName: String) extends UserPosition


sealed trait OpenCloseData

case class Open(lastChange: Long) extends OpenCloseData

case class Close(lastChange: Long) extends OpenCloseData

case class MotionDetection(lastSeen: Long)
