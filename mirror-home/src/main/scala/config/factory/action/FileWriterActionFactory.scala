package config.factory.action

import akka.http.scaladsl.model.ContentType
import model.Action

import scala.util.Try

trait FileWriterActionFactory[T] extends ActionFactory[T]

object FileWriterActionFactory {
  def apply(actionName: String, path: String, valueContentType: ContentType): FileWriterActionFactory[String] =
    new FileWriterActionFactory[String] {
      override def name: String = actionName

      override protected def oneTimeBuild(): Action[String] = new Action[String] {
        override def name: String = actionName

        override def trig(t: String): Unit = {
          import java.io.PrintWriter
          new PrintWriter(path) { write(t); close() }
        }

        override def deserialize(source: String): Try[String] = Try(source)

        override def contentType: ContentType = valueContentType
      }
    }
}