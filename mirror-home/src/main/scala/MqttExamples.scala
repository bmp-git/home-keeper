import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future

object MqttExamples extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val lastWill = MqttMessage("asd", ByteString("ohi"))
    .withQos(MqttQoS.AtLeastOnce)
    .withRetained(false)

  val connectionSettings = MqttConnectionSettings(
    "tcp://192.168.1.10:1883",
    "test-scala-client",
    new MemoryPersistence
  ).withWill(lastWill)

  val allTopics = "#"
  val topic1 = "lol"
  val mqttSource: Source[MqttMessage, Future[Done]] =
    MqttSource.atMostOnce(
      connectionSettings.withClientId(clientId = "source-spec/source"),
      MqttSubscriptions(Map(allTopics -> MqttQoS.AtLeastOnce, topic1 -> MqttQoS.AtLeastOnce)),
      bufferSize = 8
    )

  mqttSource.runForeach { m =>
    println(m)
  }

  /*val (subscribed, streamResult) = mqttSource
    .take(3)
    .toMat(Sink.seq)(Keep.both)
    .run()
  streamResult.onComplete(println)*/
}
