package model.receiver

import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

object Formats {
  implicit val receiverStatusFormat: JsonFormat[ReceiverStatus] = jsonFormat1(ReceiverStatus.apply)
}
