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
import sinks.MqttSink
import utils.{File, LocalizationService}
import webserver.{JwtUtils, RouteGenerator}

import scala.concurrent.ExecutionContextExecutor