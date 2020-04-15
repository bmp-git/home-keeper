package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Source
import model.BrokerConfig
import sources.{HttpSource, MqttSource}
import spray.json.JsonFormat

import scala.concurrent.duration.{FiniteDuration, _}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

abstract class PropertyTemplate[T: JsonFormat : ClassTag](implicit actorSystem: ActorSystem) {
  def name: String

  def mqttSource(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[T], _]

  def httpSource(request: HttpRequest, pollingFreq: FiniteDuration): Source[Try[T], _]


  def on_mqtt(topics: String*)(implicit brokerConfig: BrokerConfig): JsonPropertyFactory[T]
  = JsonPropertyFactory(name, () => mqttSource(topics: _*))

  def on_http(request: HttpRequest, pollingFreq: FiniteDuration): JsonPropertyFactory[T] =
    JsonPropertyFactory(name, () => httpSource(request, pollingFreq))

  def on_http(request: HttpRequest): JsonPropertyFactory[T] = on_http(request, 1.second)

  def map[B: JsonFormat : ClassTag](f: Try[T] => Try[B]): PropertyTemplate[B] =
    DynamicPropertyTemplateMapper(this, f)

  def filter(f: Try[T] => Boolean): PropertyTemplate[T] =
    DynamicPropertyTemplateFilter(this, f)

  def filterValue(f: T => Boolean): PropertyTemplate[T] =
    filter({
      case Success(value) if f(value) => true
      case Failure(_) => true
      case _ => false
    })

  def mapValue[B: JsonFormat : ClassTag](f: T => B): PropertyTemplate[B] = map[B]({
    case Success(value) => Success(f(value))
    case Failure(ex) => Failure(ex)
  })

  def tryMapValue[B: JsonFormat : ClassTag](f: T => Try[B]): PropertyTemplate[B] = map[B]({
    case Success(value) => f(value)
    case Failure(ex) => Failure(ex)
  })

  def recoverWith(f: Throwable => T): PropertyTemplate[T] = map[T]({
    case Failure(ex) => Success(f(ex))
    case success => success
  })

  def collect[B: JsonFormat : ClassTag](f: PartialFunction[Try[T], Try[B]]): PropertyTemplate[B] =
    filter(v => f.isDefinedAt(v)).map(f)

  def collectValue[B: JsonFormat : ClassTag](f: PartialFunction[T, B]): PropertyTemplate[B] =
    filterValue(v => f.isDefinedAt(v)).mapValue(f)
}

case class DynamicPropertyTemplate[T: JsonFormat : ClassTag](override val name: String)(implicit system: ActorSystem) extends PropertyTemplate[T]() {
  override def mqttSource(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[T], _] = {
    val string = implicitly[ClassTag[String]]
    implicitly[ClassTag[T]] match {
      case `string` => MqttSource.payloads(brokerConfig, topics: _*).map(s => s.asInstanceOf[T]).map(Success.apply)
      case _ => MqttSource.objects[T](brokerConfig, topics: _*)
    }
  }

  override def httpSource(request: HttpRequest, pollingFreq: FiniteDuration): Source[Try[T], _] = {
    val string = implicitly[ClassTag[String]]
    implicitly[ClassTag[T]] match {
      case `string` => HttpSource.bodies(request, pollingFreq).map(s => s.asInstanceOf[T]).map(Success.apply)
      case _ => HttpSource.objects[T](request, pollingFreq)
    }
  }
}

case class DynamicPropertyTemplateMapper[T: JsonFormat : ClassTag, B: JsonFormat : ClassTag](t: PropertyTemplate[T], f: Try[T] => Try[B])(implicit system: ActorSystem) extends PropertyTemplate[B]() {
  override def mqttSource(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[B], _] = t.mqttSource(topics: _*).map(f)

  override def httpSource(request: HttpRequest, pollingFreq: FiniteDuration): Source[Try[B], _] = t.httpSource(request, pollingFreq).map(f)

  override def name: String = t.name
}

case class DynamicPropertyTemplateFilter[T: JsonFormat : ClassTag](t: PropertyTemplate[T], f: Try[T] => Boolean)(implicit system: ActorSystem) extends PropertyTemplate[T]() {
  override def mqttSource(topics: String*)(implicit brokerConfig: BrokerConfig): Source[Try[T], _] = t.mqttSource(topics: _*).filter(f)

  override def httpSource(request: HttpRequest, pollingFreq: FiniteDuration): Source[Try[T], _] = t.httpSource(request, pollingFreq).filter(f)

  override def name: String = t.name
}
