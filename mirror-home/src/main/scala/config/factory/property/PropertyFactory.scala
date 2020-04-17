package config.factory.property

import config.factory.OneTimeFactory
import model.Property

trait PropertyFactory[T] extends OneTimeFactory[Property[T]] {
  def name: String
}
