package config.factory.property

import model.JsonProperty
import spray.json.JsonFormat

import scala.util.Try

class JsonValuePropertyFactory[T: JsonFormat](override val name: String, getter: () => Try[T])
  extends JsonPropertyFactory[T] {

  override def oneTimeBuild(): JsonProperty[T] = new JsonProperty[T] {

    override def name: String = JsonValuePropertyFactory.this.name

    override def value: Try[T] = getter()

    override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
  }
}
