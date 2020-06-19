package model

import scala.concurrent.duration.FiniteDuration

trait EventMatcher {
  def apply(event: Event): Boolean

  def ~(min: FiniteDuration, max: FiniteDuration) = new {
    def ~>(matcher: EventMatcher): EventRuleElement = EventRuleElement(matcher, Some(TimeMatcher(min.toMillis, max.toMillis), EventRuleElement(EventMatcher.this)))
  }

  def ~(max: FiniteDuration) = new {
    def ~>(matcher: EventMatcher): EventRuleElement = EventRuleElement(matcher, Some(TimeMatcher(0, max.toMillis), EventRuleElement(EventMatcher.this)))
  }
}

case object WifiMac extends EventMatcher {
  override def apply(event: Event): Boolean = event match {
    case _: UnknownWifiMacEvent => true
    case _ => false
  }
}

case class TimeMatcher(min: Long, max: Long) {
  def apply(lastEventTime: Long, eventTime: Long): Boolean =
    lastEventTime - max <= eventTime && eventTime <= lastEventTime - min
}

case class EventRuleElement(head: EventMatcher, tail: Option[(TimeMatcher, EventRuleElement)] = None) {
  def tailList: Seq[(TimeMatcher, EventMatcher)] = {
    tail match {
      case Some((timeMatcher, element)) => (timeMatcher, element.head) +: element.tailList
      case None => Seq()
    }
  }

  def ~(min: FiniteDuration, max: FiniteDuration) = new {
    def ~>(matcher: EventMatcher): EventRuleElement = EventRuleElement(matcher, Some(TimeMatcher(min.toMillis, max.toMillis), EventRuleElement.this))
  }

  def ~(max: FiniteDuration) = new {
    def ~>(matcher: EventMatcher): EventRuleElement = EventRuleElement(matcher, Some(TimeMatcher(0, max.toMillis), EventRuleElement.this))
  }

  def apply(events: Seq[(Long, Event)]): Seq[Seq[Event]] = Matcher(events, this)
}

object Matcher {
  def apply(events: Seq[(Long, Event)], rules: EventRuleElement): Seq[Seq[Event]] = {
    def check(rules: Seq[(TimeMatcher, EventMatcher)], events: Seq[(Long, Event)], lastEventTime: Long): Seq[Seq[Event]] = {
      rules.headOption match {
        case Some((timeMatcher, eventMatcher)) =>
          val correctTimeEvents = events.filter(e => timeMatcher(lastEventTime, e._1))
          val acceptableEvents = correctTimeEvents.filter(e => eventMatcher(e._2))
          if (acceptableEvents.isEmpty) {
            Seq() //rule failed
          } else {
            val remainingRules = rules.tail
            acceptableEvents.flatMap {
              case (eventTime, event) =>
                val remainingEvents = events.dropWhile(_ != (eventTime, event))
                check(remainingRules, remainingEvents, eventTime).map(solutions => solutions :+ event)
            }
          }
        case None => Seq(Seq()) //ok rule is completed
      }
    }

    events.headOption match {
      case Some((time, event)) if rules.head(event) =>
        check(rules.tailList, events.tail, time).map(solutions => solutions :+ event)
      case _ => Seq()
    }
  }
}


case class EventRule(matchers: EventRuleElement, guard: Home => Boolean)

object Asd extends App {

  implicit def eventRuleElement(rule: EventRuleElement)(implicit events: Seq[(Long, Event)]): Seq[Seq[Event]] = rule(events)

  val floor = Floor("", Set(), Set(), Set(), 0, "")
  val room = Room("", "", Set(), Set(), Set(), Set(), "")
  implicit val events: Seq[(Long, Event)] = Seq(
    (1000000, UnknownWifiMacEvent(floor, room, "A")),
    (1000000 - 10000, UnknownWifiMacEvent(floor, room, "B")),
    (1000000 - 20000, UnknownWifiMacEvent(floor, room, "C")))


  import scala.concurrent.duration._


  (WifiMac ~ 40.seconds ~> WifiMac) collectFirst {
    case UnknownWifiMacEvent(_, _, mac1) :: UnknownWifiMacEvent(_, _, mac2) :: Nil => println("Boom" + mac1 + mac2)
  }

}