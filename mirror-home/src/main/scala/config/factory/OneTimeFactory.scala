package config.factory

trait OneTimeFactory[T] {
  private var matBuild: Option[T] = None

  @scala.annotation.tailrec
  final def build(): T = matBuild match {
    case Some(value) => value
    case None => matBuild = Some(oneTimeBuild()); build()
  }

  protected def oneTimeBuild(): T
}
