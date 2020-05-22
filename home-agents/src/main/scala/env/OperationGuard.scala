package env

import cartago.IArtifactGuard

case class OperationGuard(name: String, numParameters: Int, guard: Array[AnyRef] => Boolean) extends IArtifactGuard {
  override def eval(objects: Array[AnyRef]): Boolean = guard(objects)

  override def getNumParameters: Int = numParameters

  override def getName: String = name
}
