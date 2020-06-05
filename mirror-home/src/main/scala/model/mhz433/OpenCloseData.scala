package model.mhz433

import akka.http.scaladsl.model.DateTime

sealed trait OpenCloseData

case class Open(lastChange: DateTime) extends OpenCloseData

case class Close(lastChange: DateTime) extends OpenCloseData

