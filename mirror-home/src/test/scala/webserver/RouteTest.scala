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
  val route: Route = RouteGenerator.generateRoutes(home, "api")

  "The mirror-world service" should {

    "return the home representation" in {
      Get("/api/home") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual homeFormat.write(home).toString()
      }
    }

    "allow to do actions" in {
      Post("/api/home/floors/first/rooms/bedroom/actions/action", "10") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual RouteGenerator.receivedPostMessage
      }
    }

    "allow to read properties" in {
      case class Timer(time: Long)
      var time = 0L
      Get("/api/home/properties/time") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        time = responseAs[String].parseJson.convertTo(jsonFormat1(Timer)).time
      }
      Thread.sleep(1000)
      Get("/api/home/properties/time") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        assert(responseAs[String].parseJson.convertTo(jsonFormat1(Timer)).time > time)
      }
    }

  }
}