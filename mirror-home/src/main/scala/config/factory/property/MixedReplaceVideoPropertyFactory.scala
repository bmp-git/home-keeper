package config.factory.property

import java.awt.image.BufferedImage

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, MediaType}
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import imgproc.Flows.mimeFrameEncoderFlow
import model.Property
import sources.RealTimeSourceMulticaster

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class MixedReplaceVideoPropertyFactory(override val name:String, multicaster:RealTimeSourceMulticaster[Option[(BufferedImage, Boolean)]])
                                      (implicit system:ActorSystem)
  extends PropertyFactory {
  override protected def oneTimeBuild(): Property = new Property {
    /*private val multicaster = RealTimeSourceMulticaster[ByteString](
      () => streamBuilder().via(mimeFrameEncoderFlow("boundary")),
      errorDefault = ByteString(),
      maxElementBuffered = 0,
      retryWhenCompleted = true)*/

    override def name: String = "video"

    override def contentType: ContentType = ContentType(MediaType.customMultipart("x-mixed-replace", Map("boundary" -> "--boundary")))

    def source(implicit executor: ExecutionContext): Try[Source[ByteString, Any]] = {
      multicaster.cast match {
        case Some(value) => Success(value.collect {
          case Some(value) => value._1
        }.via(mimeFrameEncoderFlow("boundary")))
        case None => Failure(new Exception("This stream is not active."))
      }
    }

    override def semantic: String = "video"
  }
}
