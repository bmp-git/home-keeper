package env

import cartago.{Artifact, ArtifactConfig, OPERATION}
import config.Server
import model.{DigitalTwin, Unmarshallers}
import play.api.libs.json.{JsValue, Json}
import sttp.client.quick._

class CreatorArtifact extends Artifact {
  private val apiUri = uri"${Server.uri}/api/home"
  private var json: JsValue = _

  @OPERATION def init(): Unit = {

  }

  @OPERATION def updateWorld(): Unit = {
    val response = quickRequest.get(apiUri).send()
    this.json = Json.parse(response.body)
    Unmarshallers.homeUnmarshaller(json) match {
      case Some(home) =>
        def execUpdate(name: String, dt: DigitalTwin): Unit = execLinkedOp(lookupArtifact(name), "update", dt)

        execUpdate("users_locator", home)
        execUpdate("ble_artifact", home)
        execUpdate("home", home)
        home.users.foreach(user => {
          execUpdate(s"${user.name}_smartphone", user)
        })
      case None => throw new Exception("[Error] Unexpected home json")
    }
  }

  @OPERATION def createWorld(): Unit = {
    val response = quickRequest.get(apiUri).send()
    this.json = Json.parse(response.body)
    Unmarshallers.homeUnmarshaller(json) match {
      case Some(home) =>
        makeArtifact("pages", "env.YellowPagesArtifact", new ArtifactConfig(home))
        makeArtifact("users_locator", "env.UsersLocatorArtifact", new ArtifactConfig(home))
        makeArtifact("ble_artifact", "env.BleReceiversArtifact", new ArtifactConfig(home))
        makeArtifact("home", "env.HomeArtifact", new ArtifactConfig(home))

        home.users.foreach(user => {
          makeArtifact(s"${user.name}_smartphone", "env.SmartphoneArtifact", new ArtifactConfig(user))
        })
      case None => throw new Exception("[Error] Unexpected home json")
    }
  }
}
