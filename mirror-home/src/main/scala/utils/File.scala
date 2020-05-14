package utils


import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import akka.Done

import scala.util.Try

object File {

  def read(fileName: String): Try[String] = {
    Try(scala.io.Source.fromFile(fileName)).map(source => {
      val result = Try(source.mkString)
      source.close()
      result
    }).flatten
  }

  def write(fileName: String, data: String): Try[Done] =
    Try(java.nio.file.Files.write(Paths.get(fileName), data.getBytes(StandardCharsets.UTF_8))).map(_ => Done)
}