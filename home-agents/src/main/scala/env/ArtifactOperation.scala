package env

import cartago.IArtifactOp

case class ArtifactOperation(name: String, numParameters: Int, body: Array[AnyRef] => Unit) extends IArtifactOp {

  override def exec(objects: Array[AnyRef]): Unit = body(objects)

  override def getNumParameters: Int = numParameters

  override def getName: String = name

  override def isVarArgs: Boolean = false
}
