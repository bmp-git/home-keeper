package env

import cartago.OPERATION
import model.Door

class DoorArtifact extends DigitalTwinArtifact {

  @OPERATION def init(door: Door): Unit = {
    super.dtInit(door)
    println(s"Door artifact ${this.getId.getName} created!")
  }
}
