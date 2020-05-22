package env

import cartago.{Artifact, OPERATION}
import play.api.libs.json.{JsArray, JsObject, JsString}

abstract class DigitalTwinArtifact extends Artifact {
  @OPERATION def init(json: JsObject): Unit = {
    json("properties") match {
      case JsArray(props) => {
        props.foreach({
        case JsObject(p) if p.get("value").nonEmpty => {
          defineObsProperty(p("name").as[String], p("value") match {
            case JsString(value)  =>  value
            case other  =>  other.toString
          }, p("semantic").as[String])
          println(s"Property ${p("name").as[String]} created!")
        }
        case _ =>
    })}
      case _ =>
    }


    json("actions") match {
      case JsArray(acts) => acts.foreach({
        case JsObject(a) => {
          defineOp(ArtifactOperation(a("name").as[String], 0, _ => println(s"Action ${a("name").as[String]} executed!")), OperationGuard("", 0, _ => true))}
          println(s"Action ${a("name").as[String]} created!")
        case _ =>
      })
      case _ =>
    }
  }
}
