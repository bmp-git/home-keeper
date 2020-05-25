package env

import cartago.OPERATION
import model.{Floor, User}

class UserArtifact extends DigitalTwinArtifact {

  @OPERATION def init(user: User): Unit = {
    super.dtInit(user)
    println(s"User artifact ${this.getId.getName} created!")
  }
}
