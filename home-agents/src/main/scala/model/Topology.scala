package model

import play.api.libs.json._
import utils.RichSeq._

case class Property(name: String, value: AnyRef, semantic: String)

case class Action(name: String, semantic: String)

trait Remote {
  def url: String
}

trait DigitalTwin extends Remote {
  def name: String

  def properties: Set[Property]

  def actions: Set[Action]
}

sealed trait Gateway extends DigitalTwin {
  def isPerimetral(implicit home: Home): Boolean = rooms match {
    case (r1, r2) => r1.isExternal != r2.isExternal
  }

  def rooms(implicit home: Home): (Room, Room) = {
    home.zippedRooms.filter(r => (r._2.doors ++ r._2.windows).exists(_.name == this.name)).map(_._2).toList match {
      case r1 :: r2 :: Nil => (r1, r2)
      case _ => throw new Exception("Impossible")
    }
  }

  def isOpen(implicit home: Home): Boolean = properties.find(p => p.semantic == "is_open").map(_.value.asInstanceOf[Option[OpenCloseData]]).forall {
    case Some(Open(_)) => true
    case Some(Close(_)) => false
    case None => false
  }
}

case class Door(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Window(name: String, properties: Set[Property], actions: Set[Action], url: String) extends Gateway

case class Room(name: String, floorName: String, properties: Set[Property], actions: Set[Action], doors: Set[Door], windows: Set[Window], url: String) extends DigitalTwin {

  def isInternal: Boolean = !isExternal

  def isExternal: Boolean = name == "external"

  def nonEmpty(implicit home: Home): Boolean = home.users.map(_.position).exists {
    case Some(InRoom(floor, room)) => floorName == floor && room == name
    case _ => false
  }

  def isEmpty(implicit home: Home): Boolean = !nonEmpty

  def isOneOf(rooms: Room*): Boolean = rooms.exists(r => r.name == name && r.floorName == floorName)

}

case class Floor(name: String, properties: Set[Property], actions: Set[Action], rooms: Set[Room], level: Int, url: String) extends DigitalTwin

case class User(name: String, properties: Set[Property], actions: Set[Action], url: String) extends DigitalTwin {

  def position: Option[UserPosition] = properties.find(p => p.semantic == "user_position").map(_.value.asInstanceOf[UserPosition])

  def isAtHome: Boolean = isInRoom || (position match {
    case Some(AtHome) => true
    case _ => false
  })

  def isInRoom: Boolean = position match {
    case Some(InRoom(_, _)) => true
    case _ => false
  }

}

case class Home(name: String, properties: Set[Property], actions: Set[Action], floors: Set[Floor], users: Set[User], url: String) extends DigitalTwin {
  def zippedRooms: Set[(Floor, Room)] = floors.flatMap(f => f.rooms.map(r => (f, r)))

  def zippedDoors: Set[(Floor, Room, Door)] = zippedRooms.flatMap {
    case (floor, room) => room.doors.map(d => (floor, room, d))
  }

  def zippedWindows: Set[(Floor, Room, Window)] = zippedRooms.flatMap {
    case (floor, room) => room.windows.map(w => (floor, room, w))
  }

  /*this - old*/
  def -(old: Home): Seq[Event] = {
    def gatewayFilter[T](seq: Seq[(Floor, Room, Gateway)], semantic: String): Seq[(Floor, Room, Gateway, T)] = {
      seq.flatMap {
        case (floor, room, door) => door.properties.find(_.semantic == semantic).map(p => (floor, room, door, p.value.asInstanceOf[T]))
      }
    }

    def gatewayWithProperty[PT, ET <: Event](news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)], semantic: String, g: Gateway => ET, changeFilter: (PT, PT) => Boolean): Seq[Event] = {
      val newGateways = gatewayFilter[PT](news, semantic)
      val oldGateways = gatewayFilter[PT](olds, semantic)
      newGateways.join(oldGateways, {
        case ((newFloor, newRoom, newGateway, newProperty), (oldFloor, oldRoom, oldGateway, oldProperty)) if changeFilter(newProperty, oldProperty) =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name && oldGateway.name == newGateway.name
        case _ => false
      }).distinctBy(_._1._3.name).map {
        case ((_, _, gateway, _), (_, _, _, _)) => g(gateway)
      }
    }

    def debounceTimems = 10000

    def gatewayOpened(news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)], g: Gateway => GatewayEvent): Seq[Event] = {
      gatewayWithProperty[Option[OpenCloseData], GatewayEvent](news, olds, "is_open", g, {
        case (Some(Open(_)), Some(Close(_)) | None) => true
        case (Some(Open(nt)), Some(Open(ot))) if nt > ot + debounceTimems=> true
        case (Some(Close(nt)), Some(Close(ot))) if nt > ot + debounceTimems => true
        case _ => false
      })
    }

    def gatewayMotionDetected(news: Seq[(Floor, Room, Gateway)], olds: Seq[(Floor, Room, Gateway)]): Seq[Event] = {
      gatewayWithProperty[Option[MotionDetection], GatewayMotionDetectionNearEvent](news, olds, "motion_detection", g => GatewayMotionDetectionNearEvent(g, g.rooms(this)), {
        case (Some(MotionDetection(newTime)),  Some(MotionDetection(oldTime))) if newTime > oldTime + debounceTimems => true
        case (Some(MotionDetection(_)), None) => true
        case _ => false
      })
    }

    def motionDetectionFilter(seq: Seq[(Floor, Room)]): Seq[(Floor, Room, Option[MotionDetection])] = {
      seq.flatMap {
        case (floor, room) => room.properties.find(_.semantic == "motion_detection").map(p => (floor, room, p.value.asInstanceOf[Option[MotionDetection]]))
      }
    }

    def motionDetection(news: Seq[(Floor, Room)], olds: Seq[(Floor, Room)]): Seq[Event] = {
      val newRooms = motionDetectionFilter(news)
      val oldRooms = motionDetectionFilter(olds)
      newRooms.join(oldRooms, {
        case ((newFloor, newRoom, Some(MotionDetection(newTime))), (oldFloor, oldRoom, Some(MotionDetection(oldTime)))) if newTime > oldTime + debounceTimems =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name
        case ((newFloor, newRoom, Some(MotionDetection(_))), (oldFloor, oldRoom, None)) =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name
        case _ => false
      }).map {
        case ((floor, room, _), (_, _, _)) => MotionDetectionEvent(floor, room)
      }
    }

    def userPositionFilter(users: Seq[User]): Seq[(User, UserPosition)] = {
      users.flatMap(user => user.properties.find(p => p.semantic == "user_position").map(p => (user, p.value.asInstanceOf[UserPosition])))
    }

    def backHomeUser(news: Seq[User], olds: Seq[User]): Seq[Event] = {
      val newUsers = userPositionFilter(news)
      val oldUsers = userPositionFilter(olds)

      newUsers.join(oldUsers, {
        case ((newUser, AtHome | _:InRoom), (oldUser, Unknown | Away)) => newUser.name == oldUser.name
        case ((newUser,  _:InRoom), (oldUser,  AtHome)) => newUser.name == oldUser.name
        case _ => false
      }).map {
        case ((user, _), (_,_)) => GetBackHomeEvent(user)
      }
    }

    def wifiDataFilter(room: Room): Option[TimedWifiCaptureDataSeq] = {
      room.properties.find(p => p.semantic == "wifi_receiver").map(p => p.value.asInstanceOf[TimedWifiCaptureDataSeq])
    }

    def aggregateWifiData(data: Seq[(Floor, Room)]): Seq[(Floor, Room, TimedWifiCaptureData)] = {
      data.map(r => (r._1, r._2, wifiDataFilter(r._2))).collect({
        case (floor, room, Some(data)) => (floor, room, data)
      }).flatMap({
        case (floor, room, seq) => seq.seq.filter(wd => System.currentTimeMillis() - wd.lastSeen <= 300000).map(d => (floor, room, d))
      }).groupBy({
        case (_, _, data) => data.mac
      }).map({
        case (_, datas) => datas.minBy(m => m._3.rssi)
      }).toSeq
    }

    def newWifiMacData(news: Seq[(Floor, Room)], olds: Seq[(Floor, Room)]): Seq[Event] = {
      val newWifiData = aggregateWifiData(news)
      val oldWifiData = aggregateWifiData(olds)

      newWifiData.flatMap(nd => if (oldWifiData.find(_._3.mac == nd._3.mac).fold(true)(_._3.lastSeen < nd._3.lastSeen)) Some(nd) else None).map({
        case (floor, room, data) => UnknownWifiMacEvent(floor, room, data.mac)
      })
    }

    def statusDataFilter(seq: Seq[(Floor, Room)]): Seq[(Floor, Room, ReceiverStatus)] = {
      seq.flatMap {
        case (floor, room) => room.properties.find(_.semantic == "receiver_status").map(p => (floor, room, p.value.asInstanceOf[ReceiverStatus]))
      }
    }

    def receiverOfflineEvents(news: Seq[(Floor, Room)], olds: Seq[(Floor, Room)]): Seq[ReceiverOfflineEvent] = {
      val newRooms = statusDataFilter(news)
      val oldRooms = statusDataFilter(olds)

      newRooms.join(oldRooms, {
        case ((newFloor, newRoom, ReceiverStatus(false)), (oldFloor, oldRoom, ReceiverStatus(true))) =>
          oldFloor.name == newFloor.name && oldRoom.name == newRoom.name
        case _ => false
      }).map {
        case ((floor, room, _), (_, _, _)) => ReceiverOfflineEvent(floor, room)
      }
    }


    gatewayOpened(this.zippedDoors.toSeq, old.zippedDoors.toSeq, d => GatewayOpenEvent(d, d.rooms(this))) ++
      gatewayOpened(this.zippedWindows.toSeq, old.zippedWindows.toSeq, w => GatewayOpenEvent(w, w.rooms(this))) ++
      motionDetection(this.zippedRooms.toSeq, old.zippedRooms.toSeq) ++
      gatewayMotionDetected(this.zippedDoors.toSeq, old.zippedDoors.toSeq) ++
      gatewayMotionDetected(this.zippedWindows.toSeq, old.zippedWindows.toSeq) ++
      newWifiMacData(this.zippedRooms.toSeq, old.zippedRooms.toSeq) ++
      receiverOfflineEvents(this.zippedRooms.toSeq, old.zippedRooms.toSeq) ++
      backHomeUser(this.users.toSeq, old.users.toSeq)
  }

  def isEmpty: Boolean = users.forall(!_.isAtHome)

  def everyoneAtHomeInARoom: Boolean = users.filter(_.isAtHome).forall(_.isInRoom)

  def getTime: Long = {
    this.properties.find(_.semantic == "time").map(_.value.asInstanceOf[String].toLong).getOrElse(System.currentTimeMillis())
  }
}