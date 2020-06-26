package env

import cartago.{Artifact, LINK, OPERATION}
import config.Server
import jason.asSyntax._
import model._
import org.joda.time.DateTime
import play.api.libs.json.JsBoolean
import sttp.client.quick.{quickRequest, _}
import sttp.model.Uri

import scala.concurrent.duration._

class HomeArtifact extends Artifact {

  var oldHome: Home = _
  var events: Seq[(Long, Event)] = Seq[(Long, Event)]()
  var currentEvent: Seq[Event] = Seq[Event]()
  var currentHome: Home = _

  def compute(home: Home): Seq[Term] = {
    home.properties.collectFirst {
      case Property(_, Coordinates(latitude, longitude), "location") => Seq(new NumberTermImpl(latitude), new NumberTermImpl(longitude))
    } getOrElse Nil
  }

  def isNight(time: Long): Boolean = {
    val hour = new DateTime(time).toLocalTime.getHourOfDay
    (hour > 0 && hour < 8) || hour > 21
  }

  def receivers(home: Home): Seq[ReceiverStatus] = home.zippedRooms.toSeq.flatMap(_._2.properties.find(_.semantic == "receiver_status").map(_.value.asInstanceOf[ReceiverStatus]))

  def receiverOnlineCount(home: Home): Int = {
    receivers(home).count(_.online)
  }

  def receiverCount(home: Home): Int = {
    receivers(home).size
  }

  @OPERATION def init(home: Home): Unit = {
    this.oldHome = home
    compute(home) match {
      case lat :: long :: Nil => defineObsProperty("home_location", lat, long)
      case Nil =>
    }
    defineObsProperty("users_names", home.users.map(u => new Atom(u.name)).toArray)
    val time = System.currentTimeMillis()
    defineObsProperty("time", new NumberTermImpl(time)) //TODO: time from apis
    defineObsProperty("time_slot", if (isNight(time)) new Atom("night") else new Atom("day"))
    defineObsProperty("receivers_online", new NumberTermImpl(receiverOnlineCount(home)), new NumberTermImpl(receiverCount(home)))
    defineObsProperty("events", new ListTermImpl())
  }

  @LINK def update(home: Home): Unit = {
    compute(home) match {
      case lat :: long :: Nil if !hasObsProperty("home_location") => defineObsProperty("home_location", lat, long)
      case lat :: long :: Nil if hasObsProperty("home_location") => updateObsProperty("home_location", lat, long)
      case Nil if hasObsProperty("home_location") => removeObsProperty("home_location")
      case _ =>
    }

    val time = System.currentTimeMillis()
    updateObsProperty("time", new NumberTermImpl(time))
    updateObsProperty("time_slot", if (isNight(time)) new Atom("night") else new Atom("day"))

    val events = home - oldHome
    currentHome = home
    currentEvent = events
    checkSpatialTemporalRule() //TODO: move call inside an agent
    val result = "a([" + events.map(_.toTerm).mkString(",") + "])"
    val termList = Literal.parseLiteral(result).getTerm(0)

    updateObsProperty("receivers_online", new NumberTermImpl(receiverOnlineCount(home)), new NumberTermImpl(receiverCount(home)))

    updateObsProperty("events", termList)

    this.oldHome = home
  }

  //TODO: This operation should be moved out of here
  @OPERATION def checkSpatialTemporalRule(): Unit = {
    implicit val home: Home = currentHome
    val eventsToConsume = currentEvent
    currentEvent = Seq()
    eventsToConsume.foreach(e => {
      this.events = (System.currentTimeMillis, e) +: this.events

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
