package env

import cartago.{Artifact, OPERATION}
import jason.asSyntax.Atom
import model.Home

class YellowPagesArtifact extends Artifact {

  @OPERATION def init(home: Home): Unit = {
    this.defineObsProperty("usersNames", home.users.map(u => new Atom(u.name)).toArray)
    this.defineObsProperty("floorsNames", home.floors.map(u => new Atom(u.name)).toArray)
  }
}
