package config

import java.io.File

import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox
import scala.util.Try

object Eval {
  def apply[A](string: String): Try[A] = {
    val toolbox = currentMirror.mkToolBox()
    Try {
      val tree = toolbox.parse(string)
      toolbox.eval(tree).asInstanceOf[A]
    }
  }

  def fromFile[A](file: File, imports: File): Try[A] = {
    val source = scala.io.Source.fromFile(file)
    val imp = scala.io.Source.fromFile(imports)
    val code = apply(imp.mkString("") + "\n" + source.mkString(""))
    source.close()
    imp.close()
    code
  }

  def fromFileName[A](file: String, importsFile: String): Try[A] =
    fromFile[A](new File(file), new File(importsFile))
}
