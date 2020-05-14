package imgproc

import java.awt.image.BufferedImage

import akka.stream.scaladsl.Flow
import imgproc.Flows.{broadcast2TransformAndMerge, frameToBufferedImageImageFlow, frameToIplImageFlow, iplImageToFrameImageFlow}
import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_core.IplImage

object VideoAnalysis {
  def motion_detection: Flow[Frame, BufferedImage, Any] = {
    import imgproc.RichIplImage._
    val tIdentity = Flow[IplImage]
    val backgroundFlow = Flow[IplImage].scan[Option[IplImage]](None)({
      case (Some(lastResult), image) => Some(lastResult.merge(image, 0.03))
      case (None, image) => Some(image)
    }).collect {
      case Some(image) => image
    }
    val backGroundDiffFlow = broadcast2TransformAndMerge(backgroundFlow, tIdentity,
      (background: IplImage, source: IplImage) => background absDiff source).map(_.threshold(80))
    val movDetector = broadcast2TransformAndMerge(backGroundDiffFlow, tIdentity,
      (diff: IplImage, source: IplImage) => diff.rectangles(source))

    Flow[Frame].via(frameToIplImageFlow)
      .via(movDetector)
      .via(iplImageToFrameImageFlow)
      .via(frameToBufferedImageImageFlow)
  }
}
