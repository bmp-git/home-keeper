import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import webserver.JwtUtils
import webserver.model.ModelJsonProtocol._
import webserver.model._

import scala.concurrent.Future
import scala.io.StdIn


object WebServer extends App {



  // (fake) async database query api
  var orders: List[Item] = Nil

  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o => o.id == itemId)
  }

  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _ => orders
    }
    Future {
      Done
    }
  }



  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def credentialsAuthenticator(credentials: LoginRequest): Future[Boolean] = Future {
    credentials match {
      case LoginRequest("bob", "1234") => true
      case _ => false
    }
  }

  def tokenAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case Credentials.Provided(token) => JwtUtils.verify(token).map(_.user).toOption
      case _ => None
    }

  def secured = authenticateOAuth2(realm = "secure site", tokenAuthenticator)

  val route: Route =
    concat(
      (pathPrefix("item" / LongNumber) & get) { id =>
        onSuccess(fetchItem(id)) {
          case Some(item) => complete(item)
          case None => complete(StatusCodes.NotFound)
        }
      },
      (path("create-order") & post & secured & entity(as[Order])) { (username, order) =>
        val saved: Future[Done] = saveOrder(order)
        onComplete(saved) { _ =>
          complete(s"order created by $username")
        }
      },
      (path("login") & post & entity(as[LoginRequest])) { credentials =>
        onComplete(credentialsAuthenticator(credentials)) { valid =>
          valid.toOption match {
            case Some(true) => complete(LoginResponse(JwtUtils.generate(JwtClaimData(credentials.name))))
            case _ => complete(StatusCodes.Unauthorized -> LoginResponse(""))
          }
        }
      }
    )

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}