package imgproc


import org.bytedeco.opencv.global.opencv_imgproc._
//import org.bytedeco.opencv.opencv_imgproc._
import org.bytedeco.javacpp._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core._;

//import org.bytedeco.javacv._;
object RichIplImage {

  trait SmoothType {
    def value: Int
  }

  object SmoothType {


    case object BlurNoScale extends SmoothType {
      override def value: Int = CV_BLUR_NO_SCALE
    }

    case object Blur extends SmoothType {
      override def value: Int = CV_BLUR
    }

    case object Gaussian extends SmoothType {
      override def value: Int = CV_GAUSSIAN
    }

    case object Median extends SmoothType {
      override def value: Int = CV_MEDIAN
    }

    case object Bilateral extends SmoothType {
      override def value: Int = CV_BILATERAL
    }

  }


  implicit class RichIplImage(image: IplImage) {


    private def applyBinaryOp(other: IplImage, op: (IplImage, IplImage, IplImage) => Any): IplImage = {
      val res: IplImage = image.clone()
      op(image, other, res)
      res
    }

    private def applyUnaryOp(op: (IplImage, IplImage) => Any): IplImage = {
      val res: IplImage = image.clone()
      op(image, res)
      res
    }

    def smooth(size1: Int,
               size2: Int,
               sigma1: Double,
               sigma2: Double,
               stype: SmoothType): IplImage =
      applyUnaryOp((src, dst) => cvSmooth(src, dst, stype.value, size1, size2, sigma1, sigma2))

    def absDiff(other: IplImage): IplImage =
      applyBinaryOp(other, cvAbsDiff)


    def threshold(v: Int, max: Int = 255): IplImage =
      applyUnaryOp((src, dst) => cvThreshold(src, dst, v, max, CV_THRESH_BINARY))

    def inPlaceThreshold(v: Int, max: Int = 255): IplImage = {
      cvThreshold(image, image, v, max, CV_THRESH_BINARY)
      image
    }

    def or(other: IplImage): IplImage =
      applyBinaryOp(other, cvOr)

    def merge(other: IplImage, otherV: Double): IplImage =
      applyBinaryOp(other, (src1, src2, dst) => cvAddWeighted(src1, 1 - otherV, src2, otherV, 0, dst))

    def toGrayScale: IplImage = {
      val dst = cvCreateImage(cvSize(image.width, image.height), IPL_DEPTH_8U, 1)
      cvCvtColor(image, dst, CV_BGR2GRAY)
      dst
    }

    def drawRectangle(topLeft: (Double, Double), bottomRight: (Double, Double), rgba: (Int, Int, Int), thickness: Int): IplImage = applyUnaryOp((_, dst) =>
      cvRectangle(dst, cvPoint(topLeft._1.toInt, topLeft._2.toInt), cvPoint(bottomRight._1.toInt, bottomRight._2.toInt),
        cvScalar(rgba._3, rgba._2, rgba._1, 0), thickness, 8, 0))

    def rectangles(dst:IplImage): IplImage = {
      val storage: CvMemStorage = cvCreateMemStorage()
      var contour = new CvSeq(null)

      val size = Loader.sizeof(classOf[CvContour])

      val i = image.toGrayScale
      cvFindContours(i, storage, contour, size, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0))
      i.close()

      var boxs = Seq[CvBox2D]()
      while (contour != null && !contour.isNull && contour.elem_size > 0) {
        val box = cvMinAreaRect2(contour, storage)
        contour = contour.h_next()
        if (box != null) {
          boxs = boxs :+ box
        }
      }
      storage.close()


      val r = boxs.map(box => (box.center.x - box.size.width / 2,
        box.center.y - box.size.height / 2,
        box.center.x + box.size.width / 2,
        box.center.y + box.size.height / 2)).foldLeft[Option[(Float, Float, Float, Float)]](None)({
        case (None, r) => Some(r)
        case (Some((x0, y0, x1, y1)), (x2, y2, x3, y3)) => Some((math.min(x0, x2), math.min(y0, y2), math.max(x1, x3), math.max(y1, y3)))
      })

      //val dst = cvCreateImage(cvSize(image.width, image.height), IPL_DEPTH_8U, 3)
      r match {
        case Some(value) => dst.drawRectangle((value._1, value._2),
          (value._3, value._4),
          (255, 0, 0), 2)
        case None => dst
      }
    }
  }
}
