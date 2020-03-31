package utils

class Lazy[T](wrp: => T) {
  lazy val value: T = wrp
}
