import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import config.ConfigDsl
import config.ConfigDsl._
import config.factory.ble.BleBeaconFactory
import config.factory.property.JsonPropertyFactory
import model.coordinates.Coordinates
import model.{BrokerConfig, Home}
import org.bytedeco.ffmpeg.global.avutil
import utils.{File, LocalizationService}
import webserver.{JwtUtils, RouteGenerator}
import webserver.json.JsonModel._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure

object DebugMain extends App {

  avutil.av_log_set_level(avutil.AV_LOG_QUIET)
  implicit val system: ActorSystem = ConfigDsl.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val broker: BrokerConfig = BrokerConfig("192.168.1.10")
  implicit val localizationService: LocalizationService = LocalizationService(
  port = 8086,
  gmail = "bmpprogetti@gmail.com",
  cookieFile = ".google_maps_cookie")

val edobrb = user("Edoardo", "Barbieri")
val panchh = user("Emanuele", "Pancisi")
val lory696 = user("Lorenzo", "Mondani")

  edobrb.properties(smartphone(owner = edobrb))
  panchh.properties(smartphone(owner = panchh))
  lory696.properties(smartphone(owner = lory696))
  /*val localStream = FrameSource.video("http://192.168.1.237/video.cgi").via(motion_detection)
  val localVideo = MixedReplaceVideoPropertyFactory("video", () => localStream)

  val remoteStream = FrameSource.video("http://185.39.101.26/mjpg/video.mjpg").via(motion_detection)
  val remoteVideo = MixedReplaceVideoPropertyFactory("video", () => remoteStream)*/

implicit val beacons: Seq[BleBeaconFactory] = Seq(
  ble_beacon("74daeaac2a2d", "SimpleBLEBroadca", edobrb),
  ble_beacon("abcdef123456", "not_existing_beacon", panchh))

val external = room().properties(video_motion_detection("video", "http://192.168.1.237/video.cgi"))
/*.withProperties(remoteVideo)*/
val cucina = room()
val cameraDaLetto = room()
/*.withProperties(localVideo)*/
val corridoio = room()
val bagnoRosa = room()
val bagnoVerde = room().properties(JsonPropertyFactory.dynamic[Int]("FailedProp", () => Failure(new Exception("failed")), "nothing"))
val cameraMia = room().properties(receiver("1", "fcf5c40e2540"): _*)
val ripostiglio = room()
val sala = room()

val disimpegno = room()
val bagnoMarrone = room()


val myHome = home()(
  floor("firstfloor", 0).properties(time_now(), tag("Tag", 10)).actions(trig("loll", println("lol")))(
    cucina,
    cameraDaLetto,
    corridoio.properties(pir_433_mhz("pir", "022623")),
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
  floor("basement", -1)().actions(trig("trigAction", println("trigAction")),
    turn("turnAction", b => println("turnAction: " + b)))
)
  .actions(turn("siren", b => println("siren: " + b)))
  .properties(location(44.006235, 12.116960))
  .users(edobrb, panchh, lory696)

door(sala -> external)
door(sala -> corridoio).properties(open_closed_433_mhz("magneto1", open_code = "022623", closed_code = "022629"))
door(sala -> cucina)
door(ripostiglio -> corridoio)
door(bagnoVerde -> corridoio)
door(bagnoRosa -> corridoio)
door(cameraDaLetto -> corridoio)
door(cameraMia -> corridoio)
door(ripostiglio -> external)
door(cameraMia -> external).properties(pir_433_mhz("pir", "022623"))
door(cucina -> external)
door(cameraDaLetto -> external)

window(sala -> external)
window(sala -> external)
window(bagnoVerde -> external)
window(bagnoRosa -> external)

door(disimpegno -> bagnoMarrone)

cucina.properties(time_now(), tag("lol", 20))
myHome.properties(time_now())

val build: Home = myHome.build()


val route = RouteGenerator.generateRoutes(build, "api")(if(File.exists(JwtUtils.secretKeyPath)) JwtUtils.secured else JwtUtils.unsecured)


val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
println(s"Server online at http://localhost:8090/")


Main.userCmd()
bindingFuture
  .flatMap(_.unbind())
  .onComplete(_ => system.terminate())
}
