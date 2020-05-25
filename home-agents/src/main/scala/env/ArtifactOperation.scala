package env

import cartago.IArtifactOp
import config.Server
import sttp.client.quick._
import sttp.model.Uri

import scala.util.Try

case class ArtifactOperation(name: String, numParameters: Int, body: Array[AnyRef] => Unit) extends IArtifactOp {

  override def exec(objects: Array[AnyRef]): Unit = body(objects)

  override def getNumParameters: Int = numParameters

  override def getName: String = name

  override def isVarArgs: Boolean = false
}

case class TrigOperation(name: String, url: String) extends IArtifactOp {
  override def exec(objects: Array[AnyRef]): Unit = {
    println(s"POST on ${Server.uri}$url ...")
    Uri.parse(s"${Server.uri}$url") match {
      case Left(_) =>  println(s"Failed to parse uri ${Server.uri}$url")
      case Right(value) => Try(quickRequest.post(value).send())
    }
  }

  override def getNumParameters: Int = 0

  override def getName: String = name

  override def isVarArgs: Boolean = false
}
