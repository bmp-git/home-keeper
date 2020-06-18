package imgproc

import java.awt.image.BufferedImage

import akka.stream.scaladsl.Flow
import imgproc.Flows.{broadcast2TransformAndMerge2, frameToBufferedImageImageFlow, frameToIplImageFlow, iplImageToFrameImageFlow}
import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_core.IplImage

object VideoAnalysis {
  def motion_detection(alpha: Double = 0.03): Flow[Frame, (BufferedImage, Boolean), Any] = {
    import imgproc.RichIplImage._
    def tIdentity = Flow[IplImage]

    val backgroundFlow = Flow[IplImage].scan[Option[IplImage]](None)({
      case (Some(background), image) => Some(background.merge(image, alpha))
      case (None, image) => Some(image)
    }).collect {
      case Some(image) => image
    }
    val backgroundDiffFlow = broadcast2TransformAndMerge2(backgroundFlow, tIdentity,
      (background: IplImage, image: IplImage) => background absDiff image).map(_.threshold(80))
    val motionDetectionFlow = broadcast2TransformAndMerge2(backgroundDiffFlow, tIdentity,
      (diff: IplImage, image: IplImage) => diff rectangles image)

    val transformToBufferedImage = broadcast2TransformAndMerge2(
      Flow[(IplImage, Boolean)].map(_._1).via(iplImageToFrameImageFlow).via(frameToBufferedImageImageFlow),
      Flow[(IplImage, Boolean)].map(_._2),
      (image: BufferedImage, bool: Boolean) => (image, bool))

    Flow[Frame].via(frameToIplImageFlow)
      .via(motionDetectionFlow)
      .via(transformToBufferedImage)

  }
}
