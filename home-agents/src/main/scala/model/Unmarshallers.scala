package model

import play.api.libs.json.{JsArray, JsDefined, JsLookupResult, JsString, JsValue, Reads}

object Unmarshallers {
  type Unmarshaller[-I, +O] = I => Option[O]
  type JsonUnmarshaller[+O] = Unmarshaller[JsValue, O]

  implicit class RichUnmarshaller[I, O](p: Unmarshaller[I, O]) {
    def map[T](f: O => T): Unmarshaller[I, T] = i => p(i).map(f)
  }

  implicit def fromJsLookupResultToOption(res: JsLookupResult): Option[JsValue] = Some(res) collect {
    case JsDefined(value) => value
  }

  def extract[T: Reads]: JsonUnmarshaller[T] = _.asOpt[T]

  def str(name: String): JsonUnmarshaller[String] = value[String](name)

  def number(name: String): JsonUnmarshaller[BigDecimal] = value[BigDecimal](name)

  def value[T: Reads](name: String): JsonUnmarshaller[T] = data => json(name)(data) flatMap (_.asOpt[T])

  def json(name: String): JsonUnmarshaller[JsValue] = _ \ name

  def long(name: String): JsonUnmarshaller[Long] = value[Long](name)

  def int(name: String): JsonUnmarshaller[Int] = value[Int](name)

  def double(name: String): JsonUnmarshaller[Double] = value[Double](name)

  def bool(name: String): JsonUnmarshaller[Boolean] = value[Boolean](name)

  def first[I, O](parsers: Seq[I => Option[O]]): Unmarshaller[I, O] = input =>
    (for (parser <- parsers; result <- parser(input)) yield result).headOption

  def array(name: String): JsonUnmarshaller[Seq[JsValue]] = data => data(name) match {
    case JsArray(value) => Some(value)
    case _ => None
  }

  def arrayOf[T: JsonUnmarshaller](name: String): JsonUnmarshaller[Seq[T]] = data => data(name) match {
    case JsArray(value) => Some(value.map(implicitly[JsonUnmarshaller[T]]).collect {
      case Some(v) => v
    })
    case _ => None
  }

  def anyStr(name: String): JsonUnmarshaller[String] = data => json(name)(data).map {
    case JsString(body) => body
    case a => a.toString
  }

  def dtFields: JsonUnmarshaller[(String, Set[Property], Set[Action])] = data =>
    for (name <- str("name")(data);
         actions <- arrayOf[Action]("actions")(actionUnmarshaller)(data);
         properties <- arrayOf[Property]("properties")(anyPropertyUnmarshaller).apply(data))
      yield (name, properties.toSet, actions.toSet)

  def anyPropertyUnmarshaller: JsonUnmarshaller[Property] =
    first(Seq(
      bleReceiverPropertyUnmarshaller,
      timePropertyUnmarshaller,
      locationPropertyUnmarshaller,
      smartphonePropertyUnmarshaller,
      userPositionPropertyUnmarshaller,
      isOpenPropertyUnmarshaller,
      motionDetectionPropertyUnmarshaller,
      wifiReceiverPropertyUnmarshaller,
      scannerStatusPropertyUnmarshaller,
      unknownPropertyUnmarshaller))

  def beaconDataUnmarshaller: JsonUnmarshaller[BeaconData] = data =>
    for (name <- str("user")(data);
         rssi <- int("rssi")(data);
         last_seen <- long("last_seen")(data))
      yield BeaconData(name, last_seen, rssi)

  def beaconDataSeqUnmarshaller: JsonUnmarshaller[BeaconDataSeq] = {
    case JsArray(value) =>
      Some(BeaconDataSeq(value.map(beaconDataUnmarshaller).collect {
        case Some(v) => v
      }))
    case _ => None
  }

  def wifiTimedDataUnmarshaller: JsonUnmarshaller[TimedWifiCaptureData] = data =>
    for (mac <- str("mac")(data);
         rssi <- int("rssi")(data);
         last_seen <- long("last_seen")(data))
      yield TimedWifiCaptureData(mac, rssi, last_seen)

  def wifiTimedDataSeqUnmarshaller: JsonUnmarshaller[TimedWifiCaptureDataSeq] = {
    case JsArray(value) =>
      Some(TimedWifiCaptureDataSeq(value.map(wifiTimedDataUnmarshaller).collect {
        case Some(v) => v
      }))
    case _ => None
  }

  def coordinatesUnmarshaller: JsonUnmarshaller[Coordinates] = data => {
    for(latitude <- double("latitude")(data);
        longitude <- double("longitude")(data)) yield Coordinates(latitude, longitude)
  }

  def smartphoneUnmarshaller: JsonUnmarshaller[SmartphoneData] = data => {
    for(latitude <- double("latitude")(data);
        longitude <- double("longitude")(data);
        timestamp <- long("timestamp")(data);
        accuracy <- int("accuracy")(data)) yield SmartphoneData(latitude, longitude, timestamp, accuracy)
  }

  def userPositionUnmarshaller: JsonUnmarshaller[UserPosition] = {
    first(Seq[JsonUnmarshaller[UserPosition]](
      data => for ("unknown" <- str("type")(data)) yield Unknown,
      data => for ("at_home" <- str("type")(data)) yield AtHome,
      data => for ("away" <- str("type")(data)) yield Away,
      data => for ("in_room" <- str("type")(data);
                   room <- str("room")(data);
                   floor <- str("floor")(data)) yield InRoom(floor, room)))
  }

  def openCloseDataUnmarshaller: JsonUnmarshaller[Option[OpenCloseData]] = {
    first(Seq[JsonUnmarshaller[Option[OpenCloseData]]](
      data => for (true <- bool("open")(data); time <- long("last_change")(data)) yield Some(Open(time)),
      data => for (false <- bool("open")(data); time <- long("last_change")(data)) yield Some(Close(time)),
      _ => Some(None)
    ))
  }

  def motionDetectionUnmarshaller: JsonUnmarshaller[Option[MotionDetection]] = {
    first(Seq[JsonUnmarshaller[Option[MotionDetection]]](
      data => for (time <- long("last_seen")(data)) yield Some(MotionDetection(time)),
      _ => Some(None)
    ))
  }

  def bleReceiverPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "ble_receiver" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- beaconDataSeqUnmarshaller(valueData))
      yield Property(name, value, "ble_receiver")

  def wifiReceiverPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "wifi_receiver" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- wifiTimedDataSeqUnmarshaller(valueData))
      yield Property(name, value, "wifi_receiver")

  def scannerStatusPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "receiver_status" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- onlineStatusUnmarshaller(valueData))
      yield Property(name, value, "receiver_status")

  def onlineStatusUnmarshaller: JsonUnmarshaller[ReceiverStatus] = data =>
    for (online <- bool("online")(data)) yield ReceiverStatus(online)

  def timePropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "time" <- str("semantic")(data);
         value <- str("value")(data)) yield Property(name, value, "time")

  def locationPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "location" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- coordinatesUnmarshaller(valueData)) yield Property(name, value, "location")

  def smartphonePropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "smartphone_data" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- smartphoneUnmarshaller(valueData)) yield Property(name, value, "smartphone_data")

  def userPositionPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "user_position" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- userPositionUnmarshaller(valueData)) yield Property(name, value, "user_position")

  def isOpenPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "is_open" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- openCloseDataUnmarshaller(valueData)) yield Property(name, value, "is_open")

  def motionDetectionPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         "motion_detection" <- str("semantic")(data);
         valueData <- json("value")(data);
         value <- motionDetectionUnmarshaller(valueData)) yield Property(name, value, "motion_detection")

  def unknownPropertyUnmarshaller: JsonUnmarshaller[Property] = data =>
    for (name <- str("name")(data);
         value <- anyStr("value")(data);
         semantic <- str("semantic")(data)) yield Property(name, value, semantic)

  def actionUnmarshaller: JsonUnmarshaller[Action] = data =>
    for (name <- str("name")(data);
         semantic <- str("semantic")(data)) yield Action(name, semantic)

  def doorUnmarshaller(floorName: String, roomName: String): JsonUnmarshaller[Door] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Door(name, properties, actions,
      s"/api/home/floors/$floorName/rooms/$roomName/doors/$name")

  def windowUnmarshaller(floorName: String, roomName: String): JsonUnmarshaller[Window] = data =>
    for ((name, properties, actions) <- dtFields(data)) yield Window(name, properties, actions,
      s"/api/home/floors/$floorName/rooms/$roomName/windows/$name")

  def roomUnmarshaller(floorName: String): JsonUnmarshaller[Room] = data =>
    for ((name, properties, actions) <- dtFields(data);
         doors <- arrayOf[Door]("doors")(doorUnmarshaller(floorName, name))(data);
         windows <- arrayOf[Window]("windows")(windowUnmarshaller(floorName, name))(data))
      yield Room(name, floorName, properties, actions, doors.toSet, windows.toSet, s"/api/home/floors/$floorName/rooms/$name")

  def floorUnmarshaller: JsonUnmarshaller[Floor] = data =>
    for ((name, properties, actions) <- dtFields(data);
         rooms <- arrayOf[Room]("rooms")(roomUnmarshaller(name))(data);
         level <- int("level")(data))
      yield Floor(name, properties, actions, rooms.toSet, level, s"/api/home/floors/$name")

  def userUnmarshaller: JsonUnmarshaller[User] = data =>
    for ((name, properties, actions) <- dtFields(data))
      yield User(name, properties, actions, s"/api/home/users/$name")

  def homeUnmarshaller: JsonUnmarshaller[Home] = data =>
    for ((name, properties, actions) <- dtFields(data);
         floors <- arrayOf[Floor]("floors")(floorUnmarshaller)(data);
         users <- arrayOf[User]("users")(userUnmarshaller)(data))
      yield Home(name, properties, actions, floors.toSet, users.toSet, "/api/home")
}
