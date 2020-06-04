import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import akka.stream.ActorMaterializer
import config.ConfigDsl._
import config.{ConfigDsl, Eval}
import config.factory.topology.HomeFactory
import model.Home
import webserver.RouteGenerator

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {

  implicit val system: ActorSystem = ConfigDsl.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  Eval.fromFileName[HomeFactory](RESOURCE_FOLDER + "/config.scala", RESOURCE_FOLDER + "/dependencies.scala") match {
    case Success(myHome) =>
      val build: Home = myHome.build()
      val route = RouteGenerator.generateRoutes(build, "api")
      val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
      println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
      StdIn.readLine()
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    case Failure(exception) =>
      println("Error in config file: " + exception.getMessage)
      system.terminate()
  }
}
