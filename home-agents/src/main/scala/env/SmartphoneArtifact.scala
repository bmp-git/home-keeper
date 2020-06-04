package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.{NumberTermImpl, Term}
import model.{Property, SmartphoneData, User}

class SmartphoneArtifact extends Artifact {

  def compute(user: User): Seq[Term] = {
    user.properties.collectFirst {
      case Property(_, SmartphoneData(latitude, longitude, time, accuracy), "smartphone") =>
        Seq(new NumberTermImpl(latitude), new NumberTermImpl(longitude), new NumberTermImpl(time), new NumberTermImpl(accuracy))
    } getOrElse Nil
  }

  @OPERATION def init(user: User): Unit = {
    compute(user) match {
      case lat :: long :: time :: accuracy :: Nil => defineObsProperty("smartphone", lat, long, time, accuracy)
      case Nil =>
    }
  }

  @LINK def update(user: User): Unit = {
    compute(user) match {
      case lat :: long :: time :: accuracy :: Nil if !hasObsProperty("smartphone") =>
        defineObsProperty("smartphone", lat, long, time, accuracy)
      case lat :: long :: time :: accuracy :: Nil if hasObsProperty("smartphone") =>
        updateObsProperty("smartphone", lat, long, time, accuracy)
      case Nil if hasObsProperty("smartphone") =>
        removeObsProperty("smartphone")
      case _ =>
    }
  }
}