package config.factory.property

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentType
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import model.Property

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class FileReaderPropertyFactory(override val name: String, path: String, valueContentType: ContentType, semantic: String)
                                    (implicit system: ActorSystem)
  extends PropertyFactory {
  override protected def oneTimeBuild(): Property = new Property {

    override def name: String = FileReaderPropertyFactory.this.name

    override def contentType: ContentType = valueContentType

    override def source(implicit executor: ExecutionContext): Try[Source[ByteString, Any]] = {
      if (Files.exists(Paths.get(path))) {
        Success(FileIO.fromPath(Paths.get(path)))
      } else {
        Failure(new FileNotFoundException())
      }
    }

    override def semantic: String = FileReaderPropertyFactory.this.semantic
  }
}
