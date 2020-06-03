package model

import java.io.File

import akka.http.scaladsl.model.Uri

import scala.sys.process._

case class LocalizationService(gmail: String, cookieFile: String, port: Int, workingDir: String = "../user-localizer") {
  def uri(username: String): Uri = s"http://127.0.0.1:$port/users/$username"

  def start(): Unit = {
    val outPipe: String => Unit = _ => ()
    Process("pip install -r requirements.txt", new File(workingDir)) ! ProcessLogger(outPipe, outPipe)
    val args = Seq(
      port,
      gmail,
      cookieFile)

    Process(s"python main.py ${args.mkString(" ")}", new File(workingDir)) run ProcessLogger(outPipe, outPipe)
  }

  start()

}
