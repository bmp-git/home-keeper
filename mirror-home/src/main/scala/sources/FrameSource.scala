package sources

import akka.stream.scaladsl.Source
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}

object FrameSource {
  def video(filename: String): Source[Frame, Any] = {
    Source.fromIterator[Frame](() => new Iterator[Frame] {
      val grabber = new FFmpegFrameGrabber(filename)
      grabber.start()

      override def hasNext: Boolean = true

      override def next(): Frame =
        grabber.grabImage()
    })
  }
}
