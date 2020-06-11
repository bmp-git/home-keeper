package webserver

import config.ConfigDsl._
import model.Home
import webserver.json.JsonModel._

object Utils {

  def sampleHomeConfiguration(): Home = {
    val external = room()
    val hallway = room()
    val bedroom = room().withAction(
      turn("action", println)
    )

    val h = home("home")(
      floor("first", 0)(
        hallway,
        bedroom
      )
    )
    h.withProperties(tag("myprop", "lol"))

    door(bedroom -> hallway)
    door(hallway -> external).withProperties(
      //time_now(), //don't match
      tag("color", "green"),
    )


    h.build()
  }
}
