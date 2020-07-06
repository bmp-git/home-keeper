package env

import cartago.{Artifact, LINK, OPERATION}
import config.Server
import jason.asSyntax._
import model._
import play.api.libs.json.JsBoolean
import sttp.client.quick.{quickRequest, _}
import sttp.model.Uri

import scala.concurrent.duration._

class AlarmArtifact extends Artifact {

  var events: Seq[(Long, Event)] = Seq[(Long, Event)]()
  var currentHome: Home = _
  var currentEvent: Seq[Event] = Seq[Event]()

  def eventTermList(): Term = {
    val result = "a([" + currentEvent.map(_.toTerm).mkString(",") + "])"
    Literal.parseLiteral(result).getTerm(0)
  }

  @OPERATION def init(home: Home): Unit = {
    this.currentHome = home
    defineObsProperty("events", new ListTermImpl())
  }

  @LINK def update(home: Home): Unit = {
    currentEvent = home - currentHome
    currentHome = home
    updateObsProperty("events", eventTermList())
  }


  //TODO: This operation should be moved out of here
  @OPERATION def checkSpatialTemporalRule(): Unit = {
    implicit val home: Home = currentHome
    val eventsToConsume = currentEvent
    currentEvent = Seq()
    eventsToConsume.foreach(e => {
      this.events = (home.getTime, e) +: this.events

      /** User defined rules **/
      (GatewayMotionDetectionNear ~ 40.seconds ~> GatewayOpen ~ 10.seconds ~> MotionDetectionM) (this.events) collectFirst {
        case GatewayMotionDetectionNearEvent(gateway1, (r1, r2)) :: GatewayOpenEvent(gateway2, (r3, r4)) ::
          MotionDetectionEvent(_, room) :: Nil
          if home.isEmpty &&
            gateway1.isPerimetral && gateway1.name == gateway2.name &&
            room.isOneOf(r1, r2, r3, r4) && room.isInternal =>
          turnOnAlarm()
      }

      (GatewayMotionDetectionNear ~ 40.seconds ~> MotionDetectionM) (this.events) collectFirst {
        case GatewayMotionDetectionNearEvent(g, (r1, r2)) ::
          MotionDetectionEvent(_, room) :: Nil
          if room.isEmpty &&
            g.isOpen && g.isPerimetral &&
            room.isOneOf(r1, r2) =>
          turnOnAlarm()
      }
    })
  }

  @OPERATION def turnOnAlarm(): Unit = {
    turnAlarm(JsBoolean(true).toString)
  }

  @OPERATION def turnOffAlarm(): Unit = {
    turnAlarm(JsBoolean(false).toString)
  }

  private def turnAlarm(body: String): Unit = {
    val sirenName = "siren"
    val sirenUrl = s"${Server.uri}/api/home/actions/$sirenName"
    println(s"TURNING ALARM $body on $sirenUrl ...")
    Uri.parse(sirenUrl) match {
      case Left(_) =>  println(s"Failed to parse uri $sirenUrl")
      case Right(value) => quickRequest.body(body).post(value).send()
    }
  }
}
