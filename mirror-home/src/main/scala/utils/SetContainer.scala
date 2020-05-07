package utils

case class SetContainer[T, K](content: Set[T], f: Seq[T => Any]) {
  def add(v: T): SetContainer[T, K] = {
    if(f.forall(c => !content.map(c).contains(c(v)))) {
      this.copy(content = content + v)
    } else {
      this
    }
  }

  def add(v: Traversable[T]): SetContainer[T, K] = v.foldLeft(this)((c, p) => c.add(p))
}
