package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax._
import model._

class HomeArtifact extends Artifact {

  def homeLocation(home: Home): Seq[Term] = {
    home.properties.collectFirst {
      case Property(_, Coordinates(latitude, longitude), "location") => Seq(new NumberTermImpl(latitude), new NumberTermImpl(longitude))
    } getOrElse Nil
  }

  @OPERATION def init(home: Home): Unit = {
    homeLocation(home) match {
      case lat :: long :: Nil => defineObsProperty("home_location", lat, long)
      case Nil =>
    }
  }

  @LINK def update(home: Home): Unit = {
    homeLocation(home) match {
      case lat :: long :: Nil if !hasObsProperty("home_location") => defineObsProperty("home_location", lat, long)
      case lat :: long :: Nil if hasObsProperty("home_location") => updateObsProperty("home_location", lat, long)
      case Nil if hasObsProperty("home_location") => removeObsProperty("home_location")
      case _ =>
    }
  }
}
