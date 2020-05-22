package env

import cartago.OPERATION
import play.api.libs.json.JsObject

class HomeArtifact extends DigitalTwinArtifact {

  @OPERATION override def init(home: JsObject): Unit = {
    super.init(home)
    println(s"Home artifact created!")
  }

}
