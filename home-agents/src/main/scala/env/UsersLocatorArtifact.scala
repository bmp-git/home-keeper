package env

import cartago.{Artifact, LINK, OPERATION}
import config.Server
import jason.asSyntax.{Atom, Literal, NumberTermImpl}
import model.{AtHome, Away, Home, InRoom, Unknown, User}
import play.api.libs.json.Json
import sttp.client.quick.quickRequest
import sttp.model.Uri
import sttp.client.quick._

class UsersLocatorArtifact extends Artifact {

  def userLocation(user: User): String = {
    user.properties.find(_.semantic == "user_position").map(_.value match {
      case Unknown => "unknown"
      case AtHome => "at_home"
      case Away => "away"
      case InRoom(floorName, roomName) => s"room($floorName, $roomName)"
      case _ => ""
    }).getOrElse("")
  }

  def usersLocations(home: Home): Object = {
    val result = "a([" + home.users.map(u => {
      "user_location(" + u.name + ", location(" +  userLocation(u) + "))"
    }).mkString(",") + "])"
    val parsed = Literal.parseLiteral(result)
    parsed.getTerm(0)
  }

  def insideUsersCount(home: Home): Int = home.users.map(userLocation).count(l => l == "at_home" || l.contains("room("))

  @OPERATION def init(home: Home): Unit = {
    defineObsProperty("users_names", home.users.map(u => new Atom(u.name)).toArray)
    defineObsProperty("locations", usersLocations(home))
    defineObsProperty("users_at_home", new NumberTermImpl(insideUsersCount(home)))
  }

  @LINK def update(home: Home): Unit = {
    updateObsProperty("locations", usersLocations(home))
    updateObsProperty("users_at_home", new NumberTermImpl(insideUsersCount(home)))
  }

  private def postUserPosition(user: String, body: String): Unit = {
    val actionUrl = s"${Server.uri}/api/home/users/$user/actions/position"
    println(s"UPDATE_POSITION on $actionUrl ...")
    Uri.parse(s"$actionUrl") match {
      case Left(_) =>  println(s"Failed to parse uri $actionUrl")
      case Right(value) => quickRequest.body(body).post(value).send()
    }
  }

  @OPERATION def updateUserHomePosition(user: String, floorName: String, roomName: String): Unit = {
    val js = Json.obj("type" -> "in_room", "floor" -> floorName, "room" -> roomName)
    postUserPosition(user, js.toString)
  }

  @OPERATION def updateUserPosition(user: String, place: String): Unit = {
    val js = Json.obj("type" -> place)
    postUserPosition(user, js.toString)
  }
}