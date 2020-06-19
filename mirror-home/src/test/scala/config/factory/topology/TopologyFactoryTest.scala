package config.factory.topology

import config.factory.property.JsonPropertyFactory
import model.JsonProperty
import org.scalatest.FunSuite
import spray.json.DefaultJsonProtocol._

class TopologyFactoryTest extends FunSuite {
  test("Same property name") {
    val factory = HomeFactory("my-home")
      .properties(JsonPropertyFactory.static("my-property", 1234, "none"))
      .properties(JsonPropertyFactory.static("my-property", 1235, "none"))
    assert(factory.build().properties.size == 1)
    assert(factory.build().properties.head.asInstanceOf[JsonProperty[Int]].value.get == 1234)
  }
  test("Same topology name") {
    val factory = HomeFactory("my-home")
      .floors(FloorFactory("my-floor", 0)
        .rooms(RoomFactory("my-room")
          .gateways(DoorFactory("door", (RoomFactory("f"), RoomFactory("f"))))
          .gateways(DoorFactory("door", (RoomFactory("f"), RoomFactory("f")))))
        .rooms(RoomFactory("my-room")))
      .floors(FloorFactory("my-floor", 0))

    val build = factory.build()
    assert(build.name == "my-home")
    assert(build.floors.size == 1)
    assert(build.floors.head.rooms.size == 1)
    assert(build.floors.head.rooms.head.gateways.size == 1)
  }
  test("Home factory test") {
    val factory = HomeFactory("my-home")
      .floors(FloorFactory("my-floor", 0))
      .properties(JsonPropertyFactory.static("my-property", 1234, "none"))
    val build = factory.build()
    assert(build.name == "my-home")
    assert(build.floors.exists(_.name == "my-floor"))
    assert(build.properties.exists(_.name == "my-property"))
    assert(build.properties.find(_.name == "my-property").get.asInstanceOf[JsonProperty[Int]].value.get == 1234)
  }
  test("Floor factory test") {
    val factory = FloorFactory("my-floor", 0)
      .rooms(RoomFactory("my-room"))
      .properties(JsonPropertyFactory.static("my-property", 1234, "none"))
    val build = factory.build()
    assert(build.name == "my-floor")
    assert(build.rooms.exists(_.name == "my-room"))
    assert(build.properties.exists(_.name == "my-property"))
    assert(build.properties.find(_.name == "my-property").get.asInstanceOf[JsonProperty[Int]].value.get == 1234)
  }
  test("Room factory test") {
    val r1 = RoomFactory("r1")
    val r2 = RoomFactory("r2")
    val factory = RoomFactory("my-room")
      .gateways(DoorFactory("my-door", (r1, r2)))
      .properties(JsonPropertyFactory.static("my-property", 1234, "none"))
    val build = factory.build()
    assert(build.name == "my-room")
    assert(build.gateways.exists(_.name == "my-door"))
    assert(build.properties.exists(_.name == "my-property"))
    assert(build.properties.find(_.name == "my-property").get.asInstanceOf[JsonProperty[Int]].value.get == 1234)
  }
  test("Gateways factory test") {
    val r1 = RoomFactory("r1")
    val r2 = RoomFactory("r2")
    val r3 = RoomFactory("r3")
    val external = RoomFactory("external")
    val factory = FloorFactory("my-floor", 0)
      .rooms(r1, r2, r3)
    DoorFactory("d1", (r1, r2))
    DoorFactory("d2", (r2, r3))
    WindowFactory("w1", (r1, external))

    val build = factory.build()
    assert(build.name == "my-floor")
    assert(build.rooms.exists(_.name == "r1"))
    assert(build.rooms.exists(_.name == "r2"))
    assert(build.rooms.exists(_.name == "r3"))
    assert(!build.rooms.exists(_.name == "external"))
    assert(build.rooms.find(_.name == "r1").get.gateways.exists(_.name == "d1"))
    assert(build.rooms.find(_.name == "r3").get.gateways.exists(_.name == "d2"))
    assert(build.rooms.find(_.name == "r2").get.gateways.exists(_.name == "d1"))
    assert(build.rooms.find(_.name == "r2").get.gateways.exists(_.name == "d2"))
    assert(build.rooms.find(_.name == "r1").get.gateways.exists(_.name == "w1"))
    assert(build.rooms.find(_.name == "r1").get.gateways.find(_.name == "w1").get.rooms._2.name == "external")
  }

}
