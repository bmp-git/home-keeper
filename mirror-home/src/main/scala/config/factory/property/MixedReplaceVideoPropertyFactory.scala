package config.factory.property

import java.awt.image.BufferedImage

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, MediaType}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import imgproc.Flows.mimeFrameEncoderFlow
import model.Property
import sources.RealTimeSourceMulticaster

import scala.util.{Failure, Success, Try}

case class MixedReplaceVideoPropertyFactory(override val name:String, streamBuilder: () => Source[BufferedImage, _])
                                      (implicit system:ActorSystem)
  extends PropertyFactory[Source[ByteString,_]] {
  override protected def oneTimeBuild(): Property[Source[ByteString, Any]] = new Property[Source[ByteString, Any]] {
    private val multicaster = RealTimeSourceMulticaster[ByteString](
      () => streamBuilder().via(mimeFrameEncoderFlow("boundary")),
      errorDefault = ByteString(),
      maxElementBuffered = 0,
      retryWhenCompleted = true)

    override def name: String = "video"

    override def value: Try[Source[ByteString, Any]] = multicaster.cast match {
      case Some(stream) => Success(stream)
      case None => Failure(new Exception("Stream not available now"))
    }

    override def contentType: ContentType = ContentType(MediaType.customMultipart("x-mixed-replace", Map("boundary" -> "--boundary")))

    override def serialized: Try[Source[ByteString, Any]] = value
  }
}
