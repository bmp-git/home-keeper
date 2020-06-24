package model

import scala.concurrent.duration.{FiniteDuration, _}
import scala.reflect.ClassTag
trait EventRuleElementDsl {
  def ~>(matcher: EventMatcher): EventRuleElement
}

trait EventMatcherDsl {
  def ~(min: FiniteDuration, max: FiniteDuration): EventRuleElementDsl

  def ~(max: FiniteDuration): EventRuleElementDsl
}

trait EventMatcher extends EventMatcherDsl {
  def apply(event: Event): Boolean

  override def ~(min: FiniteDuration, max: FiniteDuration): EventRuleElementDsl =
    (matcher: EventMatcher) => EventRuleElement(matcher, Some(TimeMatcher(min.toMillis, max.toMillis), EventRuleElement(EventMatcher.this)))

  override def ~(max: FiniteDuration): EventRuleElementDsl =
    (matcher: EventMatcher) => EventRuleElement(matcher, Some(TimeMatcher(0, max.toMillis), EventRuleElement(EventMatcher.this)))
}

class TypeEventMatcher[T: ClassTag]() extends EventMatcher {
  override def apply(event: Event): Boolean = {
    event match {
      case _: T => true
      case _ => false
    }
  }
}

case object UnknownWifiMac extends TypeEventMatcher[UnknownWifiMacEvent]()

case object GatewayOpen extends TypeEventMatcher[GatewayOpenEvent]()

case object GatewayMotionDetectionNear extends TypeEventMatcher[GatewayMotionDetectionNearEvent]()

case object MotionDetectionM extends TypeEventMatcher[MotionDetectionEvent]()

case object GetBackHome extends TypeEventMatcher[GetBackHomeEvent]()

case object ReceiverOffline extends TypeEventMatcher[ReceiverOfflineEvent]()

case class TimeMatcher(min: Long, max: Long) {
  def apply(lastEventTime: Long, eventTime: Long): Boolean =
    lastEventTime - max <= eventTime && eventTime <= lastEventTime - min
}

case class EventRuleElement(head: EventMatcher, tail: Option[(TimeMatcher, EventRuleElement)] = None) extends EventMatcherDsl {
  def tailList: Seq[(TimeMatcher, EventMatcher)] = {
    tail match {
      case Some((timeMatcher, element)) => (timeMatcher, element.head) +: element.tailList
      case None => Seq()
    }
  }

  override def ~(min: FiniteDuration, max: FiniteDuration): EventRuleElementDsl =
    (matcher: EventMatcher) => EventRuleElement(matcher, Some(TimeMatcher(min.toMillis, max.toMillis), EventRuleElement.this))

  override def ~(max: FiniteDuration): EventRuleElementDsl =
    (matcher: EventMatcher) => EventRuleElement(matcher, Some(TimeMatcher(0, max.toMillis), EventRuleElement.this))

  def apply(events: Seq[(Long, Event)]): Seq[Seq[Event]] = Matcher(events, this)
}

/**
 * check(rules, events, lastEventTime) {
 *    rule <- first(rules)
 *    if(rule is null) return sequence of empty sequence
 *    acceptedEvent <- filter events with rule
 *    if(acceptedEvent is empty) return empty sequence
 *    foreach(event in acceptedEvent) {
 *      remainingEvents <- all acceptedEvent after event
 *      remainingRules <- rules without rule
 *      foreach(solution in check(remainingRules, remainingEvents, time of event)) {
 *        yield solution append event
 *      }
 *    }
 * }
 */
object Matcher {
  def apply(events: Seq[(Long, Event)], rules: EventRuleElement): Seq[Seq[Event]] = {
    def check(rules: Seq[(TimeMatcher, EventMatcher)], events: Seq[(Long, Event)], lastEventTime: Long): Seq[Seq[Event]] = {
      rules match {
        case Nil => Seq(Nil) //rule is completed
        case rules if rules.length > events.length => Nil //rule will fail anyway
        case (timeMatcher, eventMatcher) :: remainingRules =>
          events.filter { case (time, event) => timeMatcher(lastEventTime, time) && eventMatcher(event) } match {
            case Nil => Nil //rule failed
            case acceptableEvents =>
              acceptableEvents.flatMap {
                case (eventTime, event) =>
                  val remainingEvents = events.dropWhile(_ != (eventTime, event))
                  check(remainingRules, remainingEvents, eventTime).map(solutions => solutions :+ event)
              }
          }
      }
    }

    events.headOption match {
      case Some((time, event)) if rules.head(event) =>
        check(rules.tailList, events.tail, time).map(solutions => solutions :+ event)
      case _ => Seq() //rule failed
    }
  }
}


case class EventRule(matchers: EventRuleElement, guard: Home => Boolean)

object Asd extends App {

  implicit def eventRuleElementApplier(rule: EventRuleElement)(implicit events: Seq[(Long, Event)]): Seq[Seq[Event]] = rule(events)

  val floor = Floor("", Set(), Set(), Set(), 0, "")
  val room = Room("", "", Set(), Set(), Set(), Set(), "")

  implicit def home: Home = ???

  implicit val events: Seq[(Long, Event)] = Seq(
    (1000000, UnknownWifiMacEvent(floor, room, "A")),
    (1000000 - 10000, UnknownWifiMacEvent(floor, room, "B")),
    (1000000 - 20000, UnknownWifiMacEvent(floor, room, "C")))

  /*(GatewayMotionDetectionNear ~ 40.seconds ~> GatewayOpen ~ 10.seconds ~> MotionDetection) collectFirst {
    case GatewayMotionDetectionNearEvent(gateway1, (r1, r2)) ::
      GatewayOpenEvent(gateway2, (r3, r4)) ::
      MotionDetectionEvent(_, room) :: Nil
      if gateway1.isPerimetral &&
        gateway1.name == gateway2.name &&
        Set(r1.name, r2.name, r3.name, r4.name).contains(room.name) &&
        room.isInternal &&
        home.isEmpty =>
      println("Boom")
  }*/

  val asd:Unit=>Unit = _ => (UnknownWifiMac ~ 40.seconds ~> UnknownWifiMac) collectFirst {
    case UnknownWifiMacEvent(_, _, mac1) :: UnknownWifiMacEvent(_, _, mac2) :: Nil => println("Boom" + mac1 + mac2)
  }

}