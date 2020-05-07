package config.factory.property

import config.factory.OneTimeFactory
import model.Property

trait PropertyFactory extends OneTimeFactory[Property] {
  def name: String
}
