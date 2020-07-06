package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.{Atom, NumberTermImpl}
import model.Home
import org.joda.time.DateTime

class ClockArtifact extends Artifact {

  def isNight(time: Long): Boolean = {
    val hour = new DateTime(time).toLocalTime.getHourOfDay
    (hour > 0 && hour < 8) || hour > 21
  }

  @OPERATION def init(home: Home): Unit = {
    val time = home.getTime
    defineObsProperty("time", new NumberTermImpl(time))
    defineObsProperty("time_slot", if (isNight(time)) new Atom("night") else new Atom("day"))
  }

  @LINK def update(home: Home): Unit = {
    val time = home.getTime
    updateObsProperty("time", new NumberTermImpl(time))
    updateObsProperty("time_slot", if (isNight(time)) new Atom("night") else new Atom("day"))
  }
}
