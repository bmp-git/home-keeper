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
    Unmarshallers.homeParser(json) match {
      case Some(home) =>
        def execUpdate(name: String, dt: DigitalTwin): Unit = execLinkedOp(lookupArtifact(name), "update", dt)

        execUpdate("home", home)
        execUpdate("users_locator", home)
        execUpdate("ble_artifact", home)
        execUpdate("house", home)
        home.floors.foreach(floor => execUpdate(floor.name, floor))
        home.users.foreach(user => {
          //execUpdate(user.name, user)
          execUpdate(s"${user.name}_smartphone", user)
        })
        home.zippedRooms.foreach({ case (floor, room) => execUpdate(s"${floor.name}_${room.name}", room) })
        home.zippedDoors.foreach({ case (floor, room, door) => execUpdate(s"${floor.name}_${room.name}_${door.name}", door) })
        home.zippedWindows.foreach({ case (floor, room, window) => execUpdate(s"${floor.name}_${room.name}_${window.name}", window) })
      case None => println("Unexpected home json")
    }
  }

  @OPERATION def create(): Unit = {
    val response = quickRequest.get(apiUri).send()
    this.json = Json.parse(response.body)
    Unmarshallers.homeParser(json) match {
      case Some(home) =>
        makeArtifact("pages", "env.YellowPagesArtifact", new ArtifactConfig(home))
        makeArtifact("home", "env.HomeArtifact", new ArtifactConfig(home))
        makeArtifact("users_locator", "env.UsersLocatorArtifact", new ArtifactConfig(home))
        makeArtifact("ble_artifact", "env.BleReceiversArtifact", new ArtifactConfig(home))
        makeArtifact("house", "env.HouseArtifact", new ArtifactConfig(home))

        home.floors.foreach(floor => makeArtifact(floor.name, "env.FloorArtifact", new ArtifactConfig(floor)))
        home.users.foreach(user => {
          makeArtifact(user.name, "env.UserArtifact", new ArtifactConfig(user))
          makeArtifact(s"${user.name}_smartphone", "env.SmartphoneArtifact", new ArtifactConfig(user))
        })
        home.zippedRooms.foreach({ case (floor, room) => makeArtifact(s"${floor.name}_${room.name}", "env.RoomArtifact", new ArtifactConfig(room)) })
        home.zippedDoors.foreach({ case (floor, room, door) => makeArtifact(s"${floor.name}_${room.name}_${door.name}", "env.DoorArtifact", new ArtifactConfig(door)) })
        home.zippedWindows.foreach({ case (floor, room, window) => makeArtifact(s"${floor.name}_${room.name}_${window.name}", "env.WindowArtifact", new ArtifactConfig(window)) })
      case None => println("Unexpected home json")
    }
  }
}
