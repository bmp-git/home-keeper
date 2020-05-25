package env

import cartago.OPERATION
import model.Room

class RoomArtifact extends DigitalTwinArtifact {

  @OPERATION def init(room: Room): Unit = {
    super.dtInit(room)
    println(s"Room artifact ${this.getId.getName} created!")
  }
}
