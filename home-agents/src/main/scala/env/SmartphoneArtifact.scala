package env

import cartago.{Artifact, LINK, OPERATION}
import jason.asSyntax.{NumberTermImpl, Term}
import model.{Property, SmartphoneData, User}

class SmartphoneArtifact extends Artifact {

  def userSmartphoneData(user: User): Seq[Term] = {
    user.properties.collectFirst {
      case Property(_, SmartphoneData(latitude, longitude, time, accuracy), "smartphone_data") =>
        Seq(new NumberTermImpl(latitude), new NumberTermImpl(longitude), new NumberTermImpl(time), new NumberTermImpl(accuracy))
    } getOrElse Nil
  }

  @OPERATION def init(user: User): Unit = {
    userSmartphoneData(user) match {
      case lat :: long :: time :: accuracy :: Nil => defineObsProperty("smartphone_data", lat, long, time, accuracy)
      case Nil =>
    }
  }

  @LINK def update(user: User): Unit = {
    userSmartphoneData(user) match {
      case lat :: long :: time :: accuracy :: Nil if !hasObsProperty("smartphone_data") =>
        defineObsProperty("smartphone_data", lat, long, time, accuracy)
      case lat :: long :: time :: accuracy :: Nil if hasObsProperty("smartphone_data") =>
        updateObsProperty("smartphone_data", lat, long, time, accuracy)
      case Nil if hasObsProperty("smartphone_data") =>
        removeObsProperty("smartphone_data")
      case _ =>
    }
  }
}