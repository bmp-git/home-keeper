package env

import cartago.OPERATION
import model.Home

class HomeArtifact extends DigitalTwinArtifact {

  @OPERATION def init(home: Home): Unit = {
    super.dtInit(home)
    println(s"Home artifact created!")
  }

}
