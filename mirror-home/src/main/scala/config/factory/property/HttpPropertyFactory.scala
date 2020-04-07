package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import sources.HttpSource
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

import scala.concurrent.duration._

object HttpPropertyFactory {

  def apply(name: String, request: HttpRequest, pollingFreq: FiniteDuration)
           (implicit actorSystem: ActorSystem): PropertyFactory[String] =
    PropertyFactory(name, () => HttpSource.bodies(request, pollingFreq))

  def objects[T: JsonFormat](name: String, request: HttpRequest, pollingFreq: FiniteDuration)
                            (implicit actorSystem: ActorSystem): PropertyFactory[T] =
    PropertyFactory(name, () => HttpSource.objects[T](request, pollingFreq))
}

