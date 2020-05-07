package config.factory.action

import config.factory.OneTimeFactory
import model.Action

trait ActionFactory extends OneTimeFactory[Action] {
  def name: String
}
