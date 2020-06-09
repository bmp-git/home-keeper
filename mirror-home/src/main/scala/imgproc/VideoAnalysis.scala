package imgproc

import java.awt.image.BufferedImage

import akka.NotUsed
import akka.stream.scaladsl.Flow
import imgproc.Flows.{broadcast2TransformAndMerge2, frameToBufferedImageImageFlow, frameToIplImageFlow, iplImageToFrameImageFlow}
import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_core.IplImage

object VideoAnalysis {
  def motion_detection: Flow[Frame, (BufferedImage, Boolean), Any] = {
    import imgproc.RichIplImage._
    def tIdentity = Flow[IplImage]

    val backgroundFlow = Flow[IplImage].scan[Option[IplImage]](None)({
      case (Some(lastResult), image) => Some(lastResult.merge(image, 0.03))
      case (None, image) => Some(image)
    }).collect {
      case Some(image) => image
    }
    val backGroundDiffFlow = broadcast2TransformAndMerge2(backgroundFlow, tIdentity,
      (background: IplImage, source: IplImage) => background absDiff source).map(_.threshold(80))
    val movDetector = broadcast2TransformAndMerge2(backGroundDiffFlow, tIdentity,
      (diff: IplImage, source: IplImage) => diff.rectangles(source))

    val transformToBufferedImage = broadcast2TransformAndMerge2[(IplImage, Boolean), BufferedImage, Boolean, (BufferedImage, Boolean)](
      Flow[(IplImage, Boolean)].map(_._1).via(iplImageToFrameImageFlow).via(frameToBufferedImageImageFlow),
      Flow[(IplImage, Boolean)].map(_._2),
      (image: BufferedImage, bool: Boolean) => (image, bool))

    Flow[Frame].via(frameToIplImageFlow)
      .via(movDetector)
      .via(transformToBufferedImage)

  }
}
