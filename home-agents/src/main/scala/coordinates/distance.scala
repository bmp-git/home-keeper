package coordinates

import jason.asSemantics.{DefaultInternalAction, TransitionSystem, Unifier}
import jason.asSyntax.{NumberTerm, NumberTermImpl, Term}

/**
 * Haversines distance.
 */
class distance extends DefaultInternalAction {

  private val AVERAGE_RADIUS_OF_EARTH_KM = 6371

  override def execute(ts: TransitionSystem, un: Unifier, args: Array[Term]): Object = {
    val latitude1 = args(0).asInstanceOf[NumberTerm].solve()
    val longitude1 = args(1).asInstanceOf[NumberTerm].solve()
    val latitude2 = args(2).asInstanceOf[NumberTerm].solve()
    val longitude2 = args(3).asInstanceOf[NumberTerm].solve()

    val latDistance = Math.toRadians(latitude1 - latitude2)
    val lngDistance = Math.toRadians(longitude1 - longitude2)
    val sinLat = Math.sin(latDistance / 2)
    val sinLng = Math.sin(lngDistance / 2)
    val a = sinLat * sinLat +
      (Math.cos(Math.toRadians(latitude1)) *
        Math.cos(Math.toRadians(latitude2)) *
        sinLng * sinLng)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    Boolean.box(un.unifies(args(4), new NumberTermImpl((AVERAGE_RADIUS_OF_EARTH_KM * c * 1000).toInt)))
  }
}
