import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.DateTime
import akka.stream.ActorMaterializer
import config.ConfigDsl
import config.ConfigDsl._
import config.factory.ble.BleBeaconFactory
import config.factory.property.JsonPropertyFactory
import model.ble.BeaconData
import model.coordinates.Coordinates
import model.{BrokerConfig, Home, LocalizationService}
import webserver.RouteGenerator
import webserver.json.JsonModel._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.Failure

object DebugMain extends App {


  implicit val system: ActorSystem = ConfigDsl.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val broker: BrokerConfig = BrokerConfig("192.168.1.10:1883")
  implicit val localizationService: LocalizationService = LocalizationService(
    port = 8086,
    gmail = "bmpprogetti@gmail.com",
    cookieFile = ".google_maps_cookie")

  val edobrb = user("Edoardo", "Barbieri")
  val panchh = user("Emanuele", "Pancisi")
  val lory696 = user("Lorenzo", "Mondani")

  edobrb.withProperties(smartphone(owner = edobrb))
  panchh.withProperties(smartphone(owner = panchh))
  lory696.withProperties(smartphone(owner = lory696))
  /*val localStream = FrameSource.video("http://192.168.1.237/video.cgi").via(motion_detection)
  val localVideo = MixedReplaceVideoPropertyFactory("video", () => localStream)

  val remoteStream = FrameSource.video("http://185.39.101.26/mjpg/video.mjpg").via(motion_detection)
  val remoteVideo = MixedReplaceVideoPropertyFactory("video", () => remoteStream)*/

  implicit val beacons: Seq[BleBeaconFactory] = Seq(
    ble_beacon("74daeaac2a2d", "SimpleBLEBroadca", edobrb),
    ble_beacon("abcdef123456", "not_existing_beacon", panchh))

  val external = room()
  /*.withProperties(remoteVideo)*/
  val cucina = room()
  val cameraDaLetto = room()
  /*.withProperties(localVideo)*/
  val corridoio = room()
  val bagnoRosa = room()
  val bagnoVerde = room().withProperties(JsonPropertyFactory.dynamic[Int]("FailedProp", () => Failure(new Exception("failed")), "nothing"))
  val cameraMia = room().withProperties(wifi_receiver("wifi_receiver", mac = "b4e62db21c79"), ble_receiver("ble_receiver", mac = "b4e62db21c79"))
  val ripostiglio = room()
  val sala = room()

  val disimpegno = room()
  val bagnoMarrone = room()

  import model.ble.Formats._

  val fixedBeacon1 = JsonPropertyFactory.static("ble_receiver",
    Seq(BeaconData("mario", DateTime.now, -23),
      BeaconData("luigi", DateTime.now, -43)), "ble_receiver")
  val fixedBeacon2 = JsonPropertyFactory.static("ble_receiver",
    Seq(BeaconData("mario", DateTime.now, -13),
      BeaconData("luigi", DateTime.now, -123)), "ble_receiver")

  val myHome = home("home")(
    floor("firstfloor", 0).withProperties(time_now(), tag("Tag", 10)).withAction(trig("loll", println("lol")))(
      cucina.withProperties(fixedBeacon1),
      cameraDaLetto.withProperties(fixedBeacon2),
      corridoio.withProperties(pir_433_mhz("pir", "scatta")),
      bagnoRosa,
      bagnoVerde,
      cameraMia,
      ripostiglio,
      sala,
      external
    ),
    floor("secondfloor", 1)(
      disimpegno,
      bagnoMarrone
    ),
    floor("basement", -1)().withAction(trig("trigAction", println("trigAction")),
      turn("turnAction", b => println("turnAction: " + b)))
  )
    .withAction(turn("siren", b => println("siren: " + b)))
    .withProperties(location(Coordinates(44.006235, 12.116960)))
    .withUsers(edobrb, panchh, lory696)

  door(sala -> external).withProperties(open_closed_433_mhz("magneto", open_code = "022623", closed_code = "022629"))
  door(sala -> corridoio)
  door(sala -> cucina)
  door(ripostiglio -> corridoio)
  door(bagnoVerde -> corridoio)
  door(bagnoRosa -> corridoio)
  door(cameraDaLetto -> corridoio)
  door(cameraMia -> corridoio)
  door(ripostiglio -> external)
  door(cameraMia -> external)
  door(cucina -> external)
  door(cameraDaLetto -> external)

  window(sala -> external)
  window(bagnoVerde -> external)
  window(bagnoRosa -> external)

  door(disimpegno -> bagnoMarrone)

  cucina.withProperties(time_now(), tag("lol", 20))
  myHome.withProperties(time_now())

  val build: Home = myHome.build()

  val route = RouteGenerator.generateRoutes(build, "api")

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
