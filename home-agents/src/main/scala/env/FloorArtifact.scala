package env

import cartago.{Artifact, OPERATION}
import play.api.libs.json.{JsObject, JsValue}

class FloorArtifact extends DigitalTwinArtifact {

  @OPERATION override def init(floor: JsObject): Unit = {
    super.init(floor)
    println(s"Floor artifact ${this.getId.getName} created!")
  }
}
