import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.alpakka.mqtt.MqttMessage
import akka.stream.scaladsl.{Keep, Source}
import akka.util.ByteString
import config.ConfigDsl
import config.ConfigDsl.{ble_beacon, door, floor, home, location, open_closed_433_mhz, pir_433_mhz, receiver, room, smartphone, time_now, turn, user, video_motion_detection, window}
import config.factory.ble.BleBeaconFactory
import model.{BrokerConfig, Home}
import org.bytedeco.ffmpeg.global.avutil
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite}
import sinks.MqttSink
import utils.LocalizationService
import webserver.{JwtUtils, RouteGenerator}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.sys.process.{Process, ProcessLogger}

class AlarmTest extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll {
  var system: ActorSystem = ConfigDsl.system
  implicit val broker: BrokerConfig = BrokerConfig("localhost:1883")
  var sirenIsOn = false
  var agentsProcess: Process = _

  override def beforeAll() {
    avutil.av_log_set_level(avutil.AV_LOG_QUIET)
    implicit val _system: ActorSystem = system
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    implicit val localizationService: LocalizationService = LocalizationService(
      port = 8086,
      gmail = "bmpprogetti@gmail.com",
      cookieFile = ".google_maps_cookie")

    val edobrb = user("Edoardo", "Barbieri")
    val lory696 = user("Lorenzo", "Mondani")
    val pancio96 = user("Emanuele", "Pancisi")

    edobrb.properties(smartphone(owner = edobrb))
    lory696.properties(smartphone(owner = lory696))
    pancio96.properties(smartphone(owner = pancio96))

    implicit val beacons: Seq[BleBeaconFactory] = Seq(
      ble_beacon("74daeaac2a2d", "SimpleBLEBroadca", edobrb),
      ble_beacon("abcdef123456", "not_existing_beacon", lory696))

    val (stream, movement) = video_motion_detection("external_door_video", "http://192.168.31.124/video.cgi")


    val external = room().properties(stream, movement)

    val cucina = room().properties(pir_433_mhz("pir", "C7D55C")).properties(receiver("receiver", "fcf5c40e28d8"): _*)
    val camera = room().properties(pir_433_mhz("pir", "05C55C")).properties(receiver("receiver", "fcf5c40e2540"): _*)
    val corridoio = room().properties(pir_433_mhz("pir", "7F055C"))
    val ingresso = room()
    val bagno = room().properties(pir_433_mhz("pir", "3CC55C")).properties(receiver("receiver", "fcf5c40e235c"): _*)
    val sala = room().properties(pir_433_mhz("pir", "sala_pir")).properties(receiver("receiver", "b4e62db21c79"): _*)

    val myHome = home()(
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
    ).actions(turn("siren", status => sirenIsOn = status))
      .properties(
        location(44.270688, 12.342442),
        time_now()
      )
      .users(edobrb, lory696, pancio96)

    door(ingresso -> external).properties(movement, stream, open_closed_433_mhz("magneto", open_code = "00BBF3", closed_code = "00BBF9"))
    door(ingresso -> cucina)
    door(ingresso -> sala)
    door(ingresso -> corridoio).properties(open_closed_433_mhz("magneto", open_code = "00E043", closed_code = "00E049"))
    door(corridoio -> bagno).properties(open_closed_433_mhz("magneto", open_code = "01D7D3", closed_code = "01D7D9"))
    door(corridoio -> camera).properties(open_closed_433_mhz("magneto", open_code = "027113", closed_code = "027119"))
    door(camera -> external).properties(open_closed_433_mhz("magneto", open_code = "018823", closed_code = "018829"))
    door(sala -> external).properties(pir_433_mhz("pir", "porta_sala_external_pir"), open_closed_433_mhz("magnetoo", open_code = "porta_sala_external_magneto", closed_code = "025BC9"))

    window(cucina -> external).properties(open_closed_433_mhz("magneto", open_code = "019C93", closed_code = "019C99"))
    window(bagno -> external).properties(open_closed_433_mhz("magneto", open_code = "022623", closed_code = "022629"))

    val build: Home = myHome.build()
    val route = RouteGenerator.generateRoutes(build, "api")(if (utils.File.exists(JwtUtils.secretKeyPath)) JwtUtils.secured else JwtUtils.unsecured)
    Http().bindAndHandle(route, "localhost", 8090)
    println(s"Server online at http://localhost:8090/")


  }

  override def beforeEach(): Unit = {
    sirenIsOn = false
  }

  override def afterEach(): Unit = {
    agentsProcess.destroy()
    Runtime.getRuntime.exec("taskkill /FI \"WindowTitle eq " + "MAS Console" + "*\" /T /F")
    println(s"Server terminated.")
  }

  def startMas(): Unit = {
    val outPipe: String => Unit = s => println(s)
    import scala.sys.process._
    agentsProcess = Process("gradlew.bat run", new File("../home-agents")) run ProcessLogger(outPipe, outPipe)
    Thread.sleep(10000)
  }

  def activate(code: String): Unit = {
    ConfigDsl.publish("scanner/abcdef123456/433", "{\"code\": \""+code+"\", \"pulselength\": 0, \"proto\": 0}")
  }

  test("[No one at home] external movement -> door opening -> internal movement should turn the alarm on") {
    startMas()
    activate("porta_sala_external_pir")
    Thread.sleep(4000)
    activate("porta_sala_external_magneto")
    Thread.sleep(4000)
    activate("sala_pir")
    Thread.sleep(4000)
    assert(sirenIsOn)
  }

  test("[No one in room sala and external door is open] external movement -> internal movement should turn the alarm on") {
    activate("porta_sala_external_magneto")
    Thread.sleep(4000)
    startMas()
    activate("porta_sala_external_pir")
    Thread.sleep(4000)
    activate("sala_pir")
    Thread.sleep(4000)
    assert(sirenIsOn)
  }

  test("[No one at home] sala_pir -> porta_sala_external_magneto -> porta_sala_external_pir STRANGE") {
    activate("sala_pir")
    Thread.sleep(4000)
    activate("porta_sala_external_magneto")
    Thread.sleep(4000)
    activate("porta_sala_external_pir")
    Thread.sleep(4000)
    assert(!sirenIsOn)
  }
}
