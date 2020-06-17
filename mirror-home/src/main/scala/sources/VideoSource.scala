package sources

import akka.stream.scaladsl.Source
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object VideoSource {
  def frames(filename: String)(implicit executionContext: ExecutionContext): Source[Frame, Any] = {
    Source.fromIterator[Frame](() => new Iterator[Frame] {
      val grabber = new FFmpegFrameGrabber(filename)
      var nextFrame: Option[Frame] = None
      grabber.start()

      override def hasNext: Boolean = {
        Try(utils.Concurrent.runWithTimeout(10.second)(grabber.grab())) match {
          case Failure(_) | Success(null) => nextFrame = None; false
          case Success(value) => nextFrame = Some(value); true
        }
      }

      override def next(): Frame = nextFrame match {
        case Some(value) => value
        case None => throw new NoSuchElementException()
      }
    })
  }
}
