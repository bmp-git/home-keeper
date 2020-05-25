package env

import cartago.OPERATION
import model.Floor

class FloorArtifact extends DigitalTwinArtifact {

  @OPERATION def init(floor: Floor): Unit = {
    super.dtInit(floor)
    println(s"Floor artifact ${this.getId.getName} created!")
  }
}
