package config.factory.action

import config.factory.OneTimeFactory
import model.Action
import spray.json.JsonFormat

trait ActionFactory[T] extends OneTimeFactory[Action[T]] {
  def name: String
}

object ActionFactory {
  //TODO: understand usage of actions
  def apply[T: JsonFormat](actionName: String, action: T => Unit): ActionFactory[T] = new ActionFactory[T] {
    override def name: String = actionName

    override protected def oneTimeBuild(): Action[T] = new Action[T] {
      override def name: String = actionName

      override def trig(t: T): Unit = action(t)

      override def jsonFormat: JsonFormat[T] = implicitly[JsonFormat[T]]
    }
  }
}
