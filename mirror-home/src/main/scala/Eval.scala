import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox
import java.io.File

import scala.util.Try

object Eval {
  def apply[A](string: String): Option[A] = {
    val toolbox = currentMirror.mkToolBox()
    Try {
      val tree = toolbox.parse(string)
      toolbox.eval(tree).asInstanceOf[A]
    }.toOption
  }

  def fromFile[A](file: File): Option[A] = {
    val source = scala.io.Source.fromFile(file)
    val code = apply(source.mkString(""))
    source.close()
    code
  }

  def fromFileName[A](file: String): Option[A] =
    fromFile(new File(file))
}