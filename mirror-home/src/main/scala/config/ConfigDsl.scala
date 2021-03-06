package config

import java.awt.image.BufferedImage

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import config.factory.action.{ActionFactory, FileWriterActionFactory, JsonActionFactory}
import config.factory.ble.BleBeaconFactory
import config.factory.property.{FileReaderPropertyFactory, JsonPropertyFactory, MixedReplaceVideoPropertyFactory, PropertyFactory}
import config.factory.topology._
import imgproc.VideoAnalysis
import model.Units.MacAddress
import model._
import model.ble.Formats._
import model.ble.{BeaconData, RawBeaconData}
import model.coordinates.Coordinates
import model.coordinates.Formats._
import model.mhz433.Formats._
import model.mhz433.{OpenCloseData, Raw433MhzData}
import model.motiondetection.Formats._
import model.motiondetection.MotionDetection
import model.receiver.Formats._
import model.receiver.ReceiverStatus
import model.user.position.Formats._
import model.user.position.{Unknown, UserPosition}
import model.user.smartphone.Formats._
import model.user.smartphone.SmartphoneData
import model.wifi.Formats._
import model.wifi.{TimedWifiCaptureData, WifiCaptureData}
import sinks.MqttSink
import sources.{HttpSource, MqttSource, RealTimeSourceMulticaster, VideoSource}
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat
import utils.RichTrySource._
import utils.{Lazy, LocalizationService, NamedIdDispatcher}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ConfigDsl {
  val RESOURCE_FOLDER = "resources"
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val namedIdDispatcher: NamedIdDispatcher = NamedIdDispatcher(1)

  /** TOPOLOGY DSL **/
  def user(firstname: String, surname: String): UserFactory = {
    val user = UserFactory(firstname, surname)
    user.attributes(file_attr("avatar", s"$RESOURCE_FOLDER/${user.name}_avatar.jpg", MediaType.image("jpeg", Compressible, "jpg", "jpeg", "png"), "user_avatar"))
      .attributes(var_attr[UserPosition]("position", Unknown, "user_position"))
  }

  def home(): HomeFactory = HomeFactory("home")

  def floor(name: String, level: Int): FloorFactory = FloorFactory(name, level)
    .attributes(file_attr("svg", s"$RESOURCE_FOLDER/$name.svg", ContentTypes.`text/xml(UTF-8)`, "floor_blueprint"))

  def floor(level: Int)(implicit name: sourcecode.Name): FloorFactory = floor(name.value, level)

  def room()(implicit name: sourcecode.Name): RoomFactory = room(name.value)

  def room(name: String): RoomFactory = RoomFactory(name)

  val external: RoomFactory = room("external")

  def door(rooms: (RoomFactory, RoomFactory)): DoorFactory = rooms match {
    case (roomA, roomB) =>
      val name = roomA.name + "_" + roomB.name
      door(name + namedIdDispatcher.next(name), rooms)
  }

  def door(name: String, rooms: (RoomFactory, RoomFactory)): DoorFactory =
    DoorFactory(name, rooms)

  def window(rooms: (RoomFactory, RoomFactory)): WindowFactory = rooms match {
    case (roomA, roomB) =>
      val name = roomA.name + "-" + roomB.name
      window(name + namedIdDispatcher.next(name), rooms)
  }

  def window(name: String, rooms: (RoomFactory, RoomFactory)): WindowFactory =
    WindowFactory(name, rooms)

  def ble_beacon(mac: MacAddress, secretKey: String, user: UserFactory): BleBeaconFactory =
    BleBeaconFactory(mac, secretKey, user)

  /** PREDEFINED VALUE BASED PROPERTIES **/
  def json_property[T: JsonFormat](name: String, value: () => T, semantic: String): JsonPropertyFactory[T] = JsonPropertyFactory.safeDynamic(name, value, semantic)

  def time_now(): JsonPropertyFactory[Long] = JsonPropertyFactory.safeDynamic("time", () => System.currentTimeMillis, "time")

  def tag[T: JsonFormat](name: String, value: T): JsonPropertyFactory[T] = JsonPropertyFactory.static(name, value, "tag")

  def location(latitude: Double, longitude: Double): JsonPropertyFactory[Coordinates] = JsonPropertyFactory.static("location", Coordinates(latitude, longitude), "location")

  /** PREDEFINED STREAM BASED PROPERTIES **/
  def ble_receiver(name: String, mac: MacAddress)
                  (implicit beaconsFactory: Seq[BleBeaconFactory], brokerConfig: BrokerConfig): JsonPropertyFactory[Seq[BeaconData]] = {
    val beacons = new Lazy(beaconsFactory.map(_.build())) //in order to call build while building
    json_from_mqtt[RawBeaconData](s"scanner/$mac/ble").ignoreFailures.scanValue(Seq[BeaconData]()) {
      case (data, raw) => beacons.value.find(_.mac == raw.addr) match {
        case Some(beacon) if beacon.validate(raw.advData) =>
          val beaconData = BeaconData(beacon.attachedTo.name, DateTime.now, raw.rssi)
          data.filter(_.userName != beaconData.userName) :+ beaconData
        case _ => data //ignore since a beacon with this mac is not registered or the validation fails
      }
    } asJsonProperty(name, "ble_receiver", Seq[BeaconData]())
  }

  private def macWifiList: Seq[String] = utils.File.readLines(s"$RESOURCE_FOLDER/mac_whitelist.txt").getOrElse(Seq[String]())

  def wifi_receiver(name: String, mac: MacAddress)
                   (implicit brokerConfig: BrokerConfig): JsonPropertyFactory[Seq[TimedWifiCaptureData]] = {
    json_from_mqtt[Seq[WifiCaptureData]](s"scanner/$mac/wifi").ignoreFailures.scanValue(Map[String, (Long, TimedWifiCaptureData)]())({
      case (map, captures) => captures.foldLeft(map) {
        case (map, capture) if map.contains(capture.mac) => map + (capture.mac -> (map(capture.mac)._1 + 1, capture.timed))
        case (map, capture) => map + (capture.mac -> (1, capture.timed))
      }
    }).mapValue(map => {
      val newMacList = macWifiList ++ map.collect {
        case (mac, (times_viewed, _)) if !macWifiList.contains(mac) && times_viewed > 5 => mac
      }.toSeq
      if (newMacList != macWifiList) {
        utils.File.writeLines(s"$RESOURCE_FOLDER/mac_whitelist.txt", newMacList)
      }
      map
    }).mapValue(_.map {
      case (_, (_, data)) => data
    }.toSeq).mapValue(_.filter(d => !macWifiList.contains(d.mac)))
      .asJsonProperty(name, "wifi_receiver", Seq[TimedWifiCaptureData]())
  }

  def receiver_status(name: String, mac: String)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[ReceiverStatus] =
    MqttSource.payloads(brokerConfig, s"scanner/$mac/status").map(_.toLowerCase).collect[ReceiverStatus]({
      case "online" => ReceiverStatus(true)
      case "offline" => ReceiverStatus(false)
    }).map(Success.apply).asJsonProperty(name, "receiver_status", ReceiverStatus(false))

  def receiver(name: String, mac: String)(implicit brokerConfig: BrokerConfig, beaconsFactory: Seq[BleBeaconFactory]): Seq[PropertyFactory] = {
    Seq(receiver_status(s"receiver_${name}_status", mac),
      wifi_receiver(s"wifi_receiver_$name", mac),
      ble_receiver(s"ble_receiver_$name", mac))
  }

  def open_closed_433_mhz(name: String, open_code: String, closed_code: String)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[Option[OpenCloseData]] =
    json_from_mqtt[Raw433MhzData]("scanner/+/433", "scanner/sonoff/433/RESULT").ignoreFailures.collectValue[Option[OpenCloseData]]({
      case data if data.code.toLowerCase == open_code.toLowerCase => Some(model.mhz433.Open(DateTime.now))
      case data if data.code.toLowerCase == closed_code.toLowerCase => Some(model.mhz433.Close(DateTime.now))
    }) asJsonProperty(name, "is_open", None)

  def open_closed_433_mhz(open_code: String, closed_code: String)(implicit name: sourcecode.Name, brokerConfig: BrokerConfig): JsonPropertyFactory[Option[OpenCloseData]] =
    open_closed_433_mhz(name.value, open_code, closed_code)

  def pir_433_mhz(name: String, code: String)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[Option[MotionDetection]] = {
    import model.mhz433.Formats._
    json_from_mqtt[Raw433MhzData]("scanner/+/433", "scanner/sonoff/433/RESULT").ignoreFailures.collectValue[Option[MotionDetection]]({
      case data if data.code.toLowerCase == code.toLowerCase => Some(MotionDetection(DateTime.now))
    }) asJsonProperty(name, "motion_detection", None)
  }

  def pir_433_mhz(code: String)(implicit name: sourcecode.Name, brokerConfig: BrokerConfig): JsonPropertyFactory[Option[MotionDetection]] =
    pir_433_mhz(name.value, code)

  def mqtt_bool(name: String, topic: String, caseTrue: String, caseFalse: String, semantic: String)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[Boolean] =
    payload_from_mqtt(topic).tryMapValue {
      case `caseTrue` => Success(true)
      case `caseFalse` => Success(false)
      case v => Failure(new Exception(s"boolean match error for value $v"))
    } asJsonProperty(name, semantic)

  def smartphone(owner: UserFactory, name: String = "smartphone")(implicit config: LocalizationService): JsonPropertyFactory[SmartphoneData] =
    json_from_http[SmartphoneData](HttpRequest(uri = config.uri(owner.name)), 1.second)
      .asJsonProperty(name, "smartphone_data")

  def video_motion_detection(name: String, source_feed: String): (MixedReplaceVideoPropertyFactory, JsonPropertyFactory[Option[MotionDetection]]) = {
    val multicaster = RealTimeSourceMulticaster[Option[(BufferedImage, Boolean)]](
      () => VideoSource.frames(source_feed)(system.dispatcher).via(VideoAnalysis.motion_detection()).map(Option.apply),
      errorDefault = None,
      maxElementBuffered = 10,
      retryWhenCompleted = true)

    val motionDetectionEvents: Source[Try[Option[MotionDetection]], NotUsed] = Source.fromIterator(() =>
      new Iterator[Option[Source[Option[(BufferedImage, Boolean)], Any]]] {
        override def hasNext: Boolean = true //
        override def next(): Option[Source[Option[(BufferedImage, Boolean)], Any]] = multicaster.cast
      }).flatMapConcat({
      case Some(value) => value.collect { case Some(value) => value }
      case None => Source.empty[(BufferedImage, Boolean)]
    }).map(_._2).scan[Option[MotionDetection]](None)({
      case (_, true) => Some(MotionDetection(DateTime.now))
      case (oldValue, false) => oldValue
    }).map(Success.apply)

    (MixedReplaceVideoPropertyFactory(name, multicaster),
      motionDetectionEvents asJsonProperty(name + "_md", "motion_detection", None))
  }

  def video_motion_detection(uri: String)(implicit name: sourcecode.Name): (MixedReplaceVideoPropertyFactory, JsonPropertyFactory[Option[MotionDetection]]) =
    video_motion_detection(name.value, uri)

  /** GENERIC PROPERTY UTILS **/
  implicit class JsonPropertyFactoryImplicit[T: JsonFormat](source: Source[Try[T], _]) {
    def asJsonProperty(name: String, semantic: String): JsonPropertyFactory[T] =
      JsonPropertyFactory.fromStream(name, () => source, semantic, None)

    def asJsonProperty(name: String, semantic: String, initial: T): JsonPropertyFactory[T] =
      JsonPropertyFactory.fromStream(name, () => source, semantic, Some(initial))
  }

  implicit class JsonPropertyDoubleFactoryImplicit(source: Source[Try[Double], _]) {
    def integrate: Source[Try[Double], Any] = source.mapValue(v => (System.currentTimeMillis(), v))
      .scanValue((System.currentTimeMillis(), 0.0))({
        case ((lastTime, energy), (now, watt)) => (now, energy + watt * ((now - lastTime) / 1000.0))
      }).mapValue(_._2)
  }

  /** PROPERTY SOURCES **/
  def payload_from_mqtt(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[String], NotUsed] =
    MqttSource.payloads(brokerConfig, topics: _*).map(Success.apply)

  def json_from_mqtt[T: JsonFormat](topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[T], NotUsed] =
    MqttSource.objects[T](brokerConfig, topics: _*)

  def payload_from_http[T: JsonFormat](request: HttpRequest, pollingFreq: FiniteDuration = 1.second): Source[Try[String], Cancellable] =
    HttpSource.bodies(request, pollingFreq)

  def json_from_http[T: JsonFormat](request: HttpRequest, pollingFreq: FiniteDuration = 1.second): Source[Try[T], Cancellable] =
    HttpSource.objects[T](request, pollingFreq)

  def file_reader(name: String, path: String, contentType: ContentType, semantic: String): PropertyFactory =
    FileReaderPropertyFactory(name, path, contentType, semantic)

  /** ACTIONS **/
  def json_action[T: JsonFormat : json.Schema](name: String, run: T => Unit, semantic: String): JsonActionFactory[T] = JsonActionFactory(name, run, semantic)

  def file_writer(name: String, path: String, contentType: ContentType): ActionFactory =
    FileWriterActionFactory(name, path, contentType)

  def turn(name: String, action: Boolean => Unit): JsonActionFactory[Boolean] =
    JsonActionFactory[Boolean](name, action, "turn")(implicitly[JsonFormat[Boolean]], json.Json.schema[Boolean])

  def turn(action: Boolean => Unit)(implicit name: sourcecode.Name): JsonActionFactory[Boolean] =
    turn(name.value, action)

  def trig(actionName: String, action: => Unit): ActionFactory = new ActionFactory {
    override def name: String = actionName

    override protected def oneTimeBuild(): Action = new Action {
      override def name: String = actionName

      override def contentType: ContentType = ContentTypes.NoContentType

      override def sink(implicit executor: ExecutionContext): Sink[ByteString, Future[Try[Done]]] = {
        action
        Sink.ignore.mapMaterializedValue(_.map(Success.apply))
      }

      override def semantic: String = "trig"
    }
  }

  /** ATTRIBUTES **/
  def file_attr(name: String, path: String, contentType: ContentType, semantic: String): (PropertyFactory, ActionFactory) =
    (file_reader(name, path, contentType, semantic), file_writer(name, path, contentType))

  def var_attr[T: JsonFormat : json.Schema](name: String, initialValue: T, semantic: String): (PropertyFactory, ActionFactory) =
    var_attr(name, initialValue, semantic, "update_" + semantic)

  def var_attr[T: JsonFormat : json.Schema](name: String, initialValue: T, property_semantic: String, action_semantic:String): (PropertyFactory, ActionFactory) = {
    var value: T = initialValue
    (json_property(name, () => value, property_semantic), json_action[T](name, value = _, action_semantic))
  }

  /** MQTT UTILS **/
  def publish(topic: String, payload: String)(implicit brokerConfig: BrokerConfig): Unit =
    Source.single(payload).to(MqttSink.fixedTopic(topic, brokerConfig)).run()
}


