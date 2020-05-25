package env

import cartago.{Artifact, ArtifactConfig, OPERATION}
import model.Home

class YellowPagesArtifact extends Artifact {

  @OPERATION def init(home: Home): Unit = {
    this.defineObsProperty("usersNames", home.users.map(_.name).toArray)
    this.defineObsProperty("floorsNames", home.floors.map(_.name).toArray)
  }
}
