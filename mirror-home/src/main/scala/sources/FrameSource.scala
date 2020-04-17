package sources

import akka.stream.scaladsl.Source
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object FrameSource {
  def video(filename: String): Source[Frame, Any] = {
    Source.fromIterator[Frame](() => new Iterator[Frame] {
      val grabber = new FFmpegFrameGrabber(filename)
      var nextFrame: Option[Frame] = None
      grabber.start()

      override def hasNext: Boolean = {
        import scala.concurrent.ExecutionContext.Implicits.global //TODO: maybe use a given actor system
        Try(runWithTimeout(10.second)(grabber.grab())) match {
          case Failure(_) | Success(null) => nextFrame = None; false
          case Success(value) => nextFrame = Some(value); true
        }
      }

      //TODO: move out
      def runWithTimeout[T](finiteDuration: FiniteDuration)(f: => T)(implicit executor: ExecutionContext): T = {
        Await.result(Future(f), finiteDuration)
      }

      override def next(): Frame = nextFrame match {
        case Some(value) => value
        case None => throw new NoSuchElementException()
      }
    })
  }
}
