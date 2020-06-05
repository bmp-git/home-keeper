package model.motiondetection

import spray.json.{JsNumber, JsObject, JsValue, JsonFormat}

object Formats {
  implicit val motionDetectionSchema: JsonFormat[MotionDetection] = new JsonFormat[MotionDetection] {
    override def write(obj: MotionDetection): JsValue = JsObject("last_seen" -> JsNumber(obj.lastSeen.clicks))
    override def read(json: JsValue): MotionDetection = ???
  }
}
