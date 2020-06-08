package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.{Atom, ListTermImpl, Literal, NumberTermImpl, Term}
import model.{Coordinates, Home, Property}
import org.joda.time.DateTime

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

  @OPERATION def init(home: Home): Unit = {
    this.oldHome = home
    compute(home) match {
      case lat :: long :: Nil => defineObsProperty("home_location", lat, long)
      case Nil =>
    }
    val time = System.currentTimeMillis()
    defineObsProperty("time", new NumberTermImpl(time)) //TODO: time from apis
    defineObsProperty("time_slot", if (isNight(time)) new Atom("night") else new Atom("day"))
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
    updateObsProperty("events", termList)

    this.oldHome = home
  }
}
