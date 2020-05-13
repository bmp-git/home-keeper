package utils

case class SetContainer[T](content: Set[T], f: Seq[T => Any]) {
  def add(v: T): SetContainer[T] = {
    if(f.forall(c => !content.map(c).contains(c(v)))) {
      this.copy(content = content + v)
    } else {
      this
    }
  }

  def add(v: Traversable[T]): SetContainer[T] = v.foldLeft(this)((c, p) => c.add(p))
}
