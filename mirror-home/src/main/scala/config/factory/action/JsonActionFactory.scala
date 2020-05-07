package config.factory.action

import config.factory.OneTimeFactory
import model.{Action, JsonAction}
import spray.json.JsonFormat

trait JsonActionFactory[T] extends ActionFactory

object JsonActionFactory {
  //TODO: understand usage of actions
  def apply[T: JsonFormat](actionName: String, action: T => Unit): JsonActionFactory[T] = new JsonActionFactory[T] {
    override def name: String = actionName

    override protected def oneTimeBuild(): JsonAction[T] = new JsonAction[T] {
      override def name: String = actionName

      override def trig(t: T): Unit = action(t)

      override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
    }
  }
}
