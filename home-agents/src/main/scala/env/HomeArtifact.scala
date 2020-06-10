package env

import cartago.{Artifact, LINK, OPERATION}
import config.Server
import jason.asSyntax.{Atom, ListTermImpl, Literal, NumberTermImpl, Term}
import model.{Coordinates, Home, Property, ReceiverStatus}
import org.joda.time.DateTime
import play.api.libs.json.JsBoolean
import sttp.client.quick.quickRequest
import sttp.model.Uri
import sttp.client.quick._

import scala.util.Try

class HomeArtifact extends Artifact {

  var oldHome: Home = _

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

    val result = "a([" + (home - oldHome).map(_.toTerm).mkString(",") + "])"
    val termList = Literal.parseLiteral(result).getTerm(0)

    updateObsProperty("receivers_online", new NumberTermImpl(receiverOnlineCount(home)), new NumberTermImpl(receiverCount(home)))

    updateObsProperty("events", termList)

    this.oldHome = home
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
      case Right(value) => Try(quickRequest.body(body).post(value).send())
    }
  }
}
