package env

import cartago.OPERATION
import model.{Door, Window}

class WindowArtifact extends DigitalTwinArtifact {

  @OPERATION def init(window: Window): Unit = {
    super.dtInit(window)
    println(s"Window artifact ${this.getId.getName} created!")
  }
}
