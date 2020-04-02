package utils

case class SetContainer[T, K](f: T => K, content: Set[T]) {
  def add(v: T): SetContainer[T, K] = {
    if (!content.map(f).contains(f(v))) {
      this.copy(content = content + v)
    } else {
      this
    }
  }

  def add(v: Traversable[T]): SetContainer[T, K] = v.foldLeft(this)((c, p) => c.add(p))
}
