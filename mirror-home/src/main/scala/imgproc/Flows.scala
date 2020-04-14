package imgproc

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import akka.util.ByteString
import javax.imageio.stream.{ImageOutputStream, MemoryCacheImageOutputStream}
import javax.imageio.{IIOImage, ImageIO, ImageWriteParam, ImageWriter}
import org.bytedeco.javacv.{Frame, Java2DFrameConverter, OpenCVFrameConverter}
import org.bytedeco.opencv.opencv_core.IplImage

object Flows {
  private def imageToJpegByteString(image: BufferedImage, quality: Float): ByteString = {
    val os: ByteArrayOutputStream = new ByteArrayOutputStream()
    val ios: ImageOutputStream = new MemoryCacheImageOutputStream(os)
    val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
    writer.setOutput(ios)
    val param = writer.getDefaultWriteParam
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) // Needed see javadoc
    param.setCompressionQuality(quality) // Highest quality
    writer.write(null, new IIOImage(image, null, null), param)
    ByteString.fromArray(os.toByteArray)
  }

  def frameToIplImageFlow: Flow[Frame, IplImage, _] = {
    val converter = new OpenCVFrameConverter.ToIplImage
    Flow[Frame].map(converter.convert)
  }

  def iplImageToFrameImageFlow: Flow[IplImage, Frame, _] = {
    val converter = new OpenCVFrameConverter.ToIplImage
    Flow[IplImage].map(converter.convert)
  }

  def frameToBufferedImageImageFlow: Flow[Frame, BufferedImage, _] = {
    val converter = new Java2DFrameConverter()
    Flow[Frame].map(converter.convert)
  }

  def mimeFrameEncoderFlow: Flow[BufferedImage, ByteString, _] = {
    Flow[BufferedImage].filter(_ != null).map(image => {
      val data = imageToJpegByteString(image, 1)
      ByteString("--boundary\r\n") ++
        ByteString(s"Content-length: ${data.length}\r\n") ++
        ByteString(s"Content-type: image/jpeg\r\n\r\n") ++
        data
    })
  }

  def broadcast2TransformAndMerge[I, T1, T2, O](t1: Flow[I, T1, _], t2: Flow[I, T2, _], m: Flow[(T1, T2), O, _]): Flow[I, O, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val bcast = builder.add(Broadcast[I](2))
      val zip = builder.add(Zip[T1, T2])
      bcast ~> t1 ~> zip.in0
      bcast ~> t2 ~> zip.in1
      FlowShape(bcast.in, zip.out)
    }).via(m)

  def broadcast2TransformAndMerge[I, T1, T2, O](t1: Flow[I, T1, _], t2: Flow[I, T2, _], m: (T1, T2) => O): Flow[I, O, NotUsed] =
    broadcast2TransformAndMerge(t1, t2, Flow[(T1, T2)].map({ case (t1, t2) => m(t1, t2) }))
}
