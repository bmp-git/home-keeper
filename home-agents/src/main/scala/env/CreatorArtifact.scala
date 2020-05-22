package env

import cartago.{Artifact, ArtifactConfig, OPERATION}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import sttp.client.quick._


class CreatorArtifact extends Artifact {
  private val apiUri = uri"http://127.0.0.1:8090/api/home"
  private var json: JsValue = _

  @OPERATION def init(): Unit = {

  }

  @OPERATION def create(): Unit = {
    val response = quickRequest.get(apiUri).send()
    this.json = Json.parse(response.body)
    this.json match {
      case h @ JsObject(home) => {
        makeArtifact("home", "env.HomeArtifact", new ArtifactConfig(h))
        home("floors") match {
          case JsArray(floors) => floors.foreach({
              case f : JsObject => {
                val name = f("name").as[String]
                makeArtifact(s"$name", "env.FloorArtifact", new ArtifactConfig(f))
              }
              case _ =>
            })
          case _ =>
        }
      }
      case _ => println("Unexpected home json")
    }
  }



}
