import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import config.ConfigDsl
import config.ConfigDsl.{ble_beacon, door, floor, home, location, open_closed_433_mhz, pir_433_mhz, receiver, room, smartphone, tag, time_now, trig, turn, user, video_motion_detection, window}
import config.factory.ble.BleBeaconFactory
import model.{BrokerConfig, Home, LocalizationService}
import model.coordinates.Coordinates
import org.bytedeco.ffmpeg.global.avutil
import sources.MqttSource
import spray.json.{JsNumber, JsObject, JsString}
import utils.File
import webserver.{JwtUtils, RouteGenerator}

import scala.concurrent.ExecutionContextExecutor

object MimaDeployMain extends App {

  avutil.av_log_set_level(avutil.AV_LOG_QUIET)
  implicit val system: ActorSystem = ConfigDsl.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val broker: BrokerConfig = BrokerConfig("doru2.mnd.cloud").withAuth("homekeeper", "8CUAgjwyuaJu")
  implicit val localizationService: LocalizationService = LocalizationService(
    port = 8086,
    gmail = "bmpprogetti@gmail.com",
    cookieFile = ".google_maps_cookie")

  val edobrb = user("Edoardo", "Barbieri")
  val lory696 = user("Lorenzo", "Mondani")
  val pancio96 = user("Emanuele", "Pancisi")

  edobrb.add_properties(smartphone(owner = edobrb))
  lory696.add_properties(smartphone(owner = lory696))
  pancio96.add_properties(smartphone(owner = pancio96))

  implicit val beacons: Seq[BleBeaconFactory] = Seq(
    ble_beacon("74daeaac2a2d", "SimpleBLEBroadca", edobrb),
    ble_beacon("abcdef123456", "not_existing_beacon", lory696))

  val (stream, movement) = video_motion_detection("external_door_video", "http://192.168.31.124/video.cgi")


  val external = room().add_properties(stream, movement)

  val cucina = room().add_properties(pir_433_mhz("pir", "C7D55C")).add_properties(receiver("receiver", "fcf5c40e28d8"): _*)
  val camera = room().add_properties(pir_433_mhz("pir", "05C55C")).add_properties(receiver("receiver", "fcf5c40e2540"): _*)
  val corridoio = room().add_properties(pir_433_mhz("pir", "7F055C"))
  val ingresso = room()
  val bagno = room().add_properties(pir_433_mhz("pir", "3CC55C")).add_properties(receiver("receiver", "fcf5c40e235c"): _*)
  val sala = room().add_properties(pir_433_mhz("pir", "17D55C")).add_properties(receiver("receiver", "b4e62db21c79"): _*)

  val myHome = home("home")(
    floor("mansarda", 2)(
      cucina,
      camera,
      corridoio,
      ingresso,
      bagno,
      sala,
      external
    ),
    //floor("terra", 0)
  ).add_actions(turn("siren", status => println("Turning alarm: " + status)))
    .add_properties(
      location(44.270688, 12.342442),
      time_now()
    )
    .withUsers(edobrb, lory696, pancio96)

  door(ingresso -> external).add_properties(movement, stream, open_closed_433_mhz("magneto", open_code = "00BBF3", closed_code = "00BBF9"))
  door(ingresso -> cucina)
  door(ingresso -> sala)
  door(ingresso -> corridoio).add_properties(open_closed_433_mhz("magneto", open_code = "00E043", closed_code = "00E049"))
  door(corridoio -> bagno).add_properties(open_closed_433_mhz("magneto", open_code = "01D7D3", closed_code = "01D7D9"))
  door(corridoio -> camera).add_properties(open_closed_433_mhz("magneto", open_code = "027113", closed_code = "027119"))
  door(camera -> external).add_properties(open_closed_433_mhz("magneto", open_code = "018823", closed_code = "018829"))
  door(sala -> external).add_properties(open_closed_433_mhz("magneto", open_code = "025BC3", closed_code = "025BC9"))

  window(cucina -> external).add_properties(open_closed_433_mhz("magneto", open_code = "019C93", closed_code = "019C99"))
  window(bagno -> external).add_properties(open_closed_433_mhz("magneto", open_code = "022623", closed_code = "022629"))

  val build: Home = myHome.build()


  val route = RouteGenerator.generateRoutes(build, "api")(if (File.exists(JwtUtils.secretKeyPath)) JwtUtils.secured else JwtUtils.unsecured)

  MqttSource.topicsAndPayloads(broker, "#").runForeach({
    case (topic, payload) =>
      val obj = JsObject("time" -> JsNumber(System.currentTimeMillis()), "topic" -> JsString(topic), "payload" -> JsString(payload))
      File.append(ConfigDsl.RESOURCE_FOLDER + "/mqttlog.txt", obj.toString() + "\n")
  })

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8090)
  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")


  Main.userCmd()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
