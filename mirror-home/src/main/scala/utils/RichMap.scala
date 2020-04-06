package utils

object RichMap {

  implicit class RichMap[K, V](map: Map[K, V]) {
    def addOrUpdate(p: (K, V)): Map[K, V] = p match {
      case (k, v) =>
        if (map.isDefinedAt(k)) {
          map.map {
            case (`k`, _) => k -> v
            case x => x
          }
        } else {
          map + (k -> v)
        }
    }
    def toValueSeq:Seq[V] = map.toSeq.map(_._2)
  }

}
