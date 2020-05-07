package config.factory.action

import java.nio.file.Paths

import akka.Done
import akka.http.scaladsl.model.ContentType
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import model.Action

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait FileWriterActionFactory extends ActionFactory

object FileWriterActionFactory {
  def apply(actionName: String, path: String, valueContentType: ContentType): FileWriterActionFactory =
    new FileWriterActionFactory {
      override def name: String = actionName

      override protected def oneTimeBuild(): Action = new Action {
        override def name: String = actionName

        override def contentType: ContentType = valueContentType

        override def sink(implicit executor: ExecutionContext): Sink[ByteString, Future[Try[Done]]] =
          FileIO.toPath(Paths.get(path)).mapMaterializedValue(_.map {
            case IOResult(_, result) => result
          })
      }
    }
}