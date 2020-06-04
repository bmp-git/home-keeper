import config.ConfigDsl._
import model._
import spray.json.DefaultJsonProtocol._

import config.factory.ble.BleBeaconFactory
import config.factory.property.JsonPropertyFactory
import config.factory.topology.HomeFactory
import model.ble.BeaconData
import model.coordinates.Coordinates
import akka.http.scaladsl.model.DateTime