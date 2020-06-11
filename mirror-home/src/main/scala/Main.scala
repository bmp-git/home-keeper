import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import config.ConfigDsl._
import config.factory.topology.HomeFactory
import config.{ConfigDsl, Eval}
import model.Home
import org.bytedeco.ffmpeg.global.avutil
import utils.File
import webserver.json.JwtClaimData
import webserver.{JwtUtils, RouteGenerator}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object Main extends App {

  @scala.annotation.tailrec
  def userCmd(): Unit = {
    StdIn.readLine() match {
      case "exit" => System.exit(0)
      case "jwt" =>
        print(s"Username: ")
        val username = StdIn.readLine()
        print(s"Duration (days): ")
        val days = StdIn.readLine()
        (username, Try(days.toInt)) match {
          case (username, Success(d)) if username.nonEmpty =>
            println(JwtUtils.generate(JwtClaimData(username), d.days))
          case _ => println(s"Invalid input")
        }
        userCmd()
      case "" => userCmd()
      case cmd => println(s"Invalid command $cmd"); userCmd()
    }
  }

  avutil.av_log_set_level(avutil.AV_LOG_QUIET)

  implicit val system: ActorSystem = ConfigDsl.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  Eval.fromFileName[HomeFactory](RESOURCE_FOLDER + "/config.scala", RESOURCE_FOLDER + "/dependencies.scala") match {
    case Success(myHome) =>
      val build: Home = myHome.build()
      val route = RouteGenerator.generateRoutes(build, "api")(if (File.exists(JwtUtils.secretKeyPath)) JwtUtils.secured else JwtUtils.unsecured)
      val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
      println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
      userCmd()
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    case Failure(exception) =>
      println("Error in config file: " + exception.getMessage)
      system.terminate()
  }
}
