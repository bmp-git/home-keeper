implicit val broker: BrokerConfig = BrokerConfig("192.168.1.54:1883")
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
val sala = room().properties(pir_433_mhz("pir", "17D55C")).properties(receiver("receiver", "b4e62db21c79"): _*)

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
).actions(turn("siren", status => println("Turning alarm: " + status)))
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
door(sala -> external).properties(open_closed_433_mhz("magneto", open_code = "025BC3", closed_code = "025BC9"))

window(cucina -> external).properties(open_closed_433_mhz("magneto", open_code = "019C93", closed_code = "019C99"))
window(bagno -> external).properties(open_closed_433_mhz("magneto", open_code = "022623", closed_code = "022629"))
myHome