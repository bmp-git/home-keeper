package utils

import scala.collection.mutable

case class IdDispatcher(start: Long) {
  private val idsLock = new Object()
  private val ids: Iterator[Long] = Stream.continually(1.toLong).scanLeft(start) {
    case (Long.MaxValue, _) => start
    case (id, inc) => id + inc
  }.iterator

  def next: Long = idsLock.synchronized(ids.next())
}

case class NamedIdDispatcher(start: Long) {
  private val names: mutable.Map[String, IdDispatcher] = mutable.Map[String, IdDispatcher]()

  def next(name: String): String = {
    names.get(name) match {
      case Some(dispatcher) => "-" + dispatcher.next.toString
      case None => names.put(name, IdDispatcher(start)); ""
    }
  }
}