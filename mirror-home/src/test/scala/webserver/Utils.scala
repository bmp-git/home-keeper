package webserver

import config.ConfigDsl.{door, floor, home, room, tag, time_now}
import config.factory.action.{ActionFactory, JsonActionFactory}
import model.Home
import webserver.json.JsonModel._

object Utils {

  def sampleHomeConfiguration(): Home = {
    val external = room()
    val hallway = room()
    val bedroom = room().withAction(
      JsonActionFactory[Int]("action", v => println(s"Acting with $v"))
    )

    val h = home("home")(
      floor("first")(
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
