package webserver

import config.ConfigDsl._
import model.Home
import webserver.json.JsonModel._

object Utils {

  def sampleHomeConfiguration(): Home = {
    val external = room()
    val hallway = room()
    val bedroom = room().actions(
      turn("action", println)
    )

    val h = home()(
      floor("first", 0)(
        hallway,
        bedroom
      )
    )
    h.properties(tag("myprop", "lol"))

    door(bedroom -> hallway)
    door(hallway -> external).properties(
      //time_now(), //don't match
      tag("color", "green"),
    )


    h.build()
  }
}
