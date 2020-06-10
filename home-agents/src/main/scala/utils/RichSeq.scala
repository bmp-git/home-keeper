package utils

package object RichSeq {
  implicit class RichSeq[T](seq: Seq[T]) {
    def join(other: Seq[T], on: ((T, T)) => Boolean): Seq[(T, T)] = {
      seq.flatMap(t1 => other.map(t2 => (t1, t2))).filter(on)
    }

    def distinctBy[K](f: T => K): Seq[T] = seq.groupBy(f).map(_._2.head).toSeq
  }
}
