package webserver

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import model.Home
import webserver.json.JsonModel._
import spray.json._

class RouteTest extends WordSpec with Matchers with ScalatestRouteTest {
  val home: Home = Utils.sampleHomeConfiguration()
  val route: Route = RouteGenerator.generateRoutes(home, "api")(JwtUtils.unsecured)

  "The mirror-world service" should {

    "return the home representation" in {
      Get("/api/home") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual homeFormat.write(home).toString()
      }
    }

    "allow to do actions" in {
      Post("/api/home/floors/first/rooms/bedroom/actions/action", "true") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual RouteGenerator.receivedPostMessage
      }
    }

    "allow to read properties" in {
      case class Timer(time: Long)
      Get("/api/home/properties/myprop") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        assert(responseAs[String].contains("\"name\":\"myprop\""))
        assert(responseAs[String].contains("\"semantic\":\"tag\""))
        assert(responseAs[String].contains("\"value\":\"lol\""))
      }
    }

  }
}