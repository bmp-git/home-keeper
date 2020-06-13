package utils


import java.io.FileWriter
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

  def readLines(fileName: String): Try[Seq[String]] = {
    Try(scala.io.Source.fromFile(fileName)).flatMap(source => {
      val result: Try[Seq[String]] = Try(source.getLines().toList)
      source.close()
      result
    })
  }

  def exists(fileName: String): Boolean = java.nio.file.Files.exists(Paths.get(fileName))

  def write(fileName: String, data: String): Try[Done] =
    Try(java.nio.file.Files.write(Paths.get(fileName), data.getBytes(StandardCharsets.UTF_8))).map(_ => Done)

  def writeLines(fileName: String, data: Seq[String]): Try[Done] =
    Try(java.nio.file.Files.write(Paths.get(fileName), data.mkString("\n").getBytes(StandardCharsets.UTF_8))).map(_ => Done)

  def append(fileName: String, data: String): Try[Done] =
    Try(new FileWriter(fileName, true)).flatMap(source => Try {
      source.write(data)
      source.close()
      Done
    })

}