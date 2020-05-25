package env

import cartago.{Artifact, OPERATION}
import model.Home

class BleReceiversArtifact extends Artifact {

  @OPERATION def init(home: Home): Unit = {
    this.defineObsProperty("receivers", ???)//[(), ()]
  }
}
