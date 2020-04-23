package config.factory.property

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import model.Property

import scala.util.Try

case class FileReaderPropertyFactory(override val name: String, path: String, valueContentType: ContentType)
                                    (implicit system: ActorSystem)
  extends PropertyFactory[String] {
  override protected def oneTimeBuild(): Property[String] = new Property[String] {

    override def name: String = FileReaderPropertyFactory.this.name

    override def value: Try[String] = Try {
      val source = scala.io.Source.fromFile(path)
      val lines = try source.mkString finally source.close()
      lines
    }

    override def contentType: ContentType = valueContentType

    override def serialized: Try[Source[ByteString, Any]] = value.map(content => Source.single(ByteString(content)))
  }
}
