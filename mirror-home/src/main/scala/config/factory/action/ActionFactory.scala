package config.factory.action

import config.factory.OneTimeFactory
import model.Action

trait ActionFactory[T] extends OneTimeFactory[Action[T]] {
  def name: String
}
