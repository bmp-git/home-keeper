package utils

import java.security.MessageDigest

import spire.math.ULong

import scala.util.Try

object Crypto {
  def parseULong(hexString: String): Option[ULong] = {
    Try(ULong.fromBigInt(BigInt(hexString, 0x10))).toOption
  }

  def toHexString(data: Iterable[Byte]): String = {
    val sb = new StringBuilder
    for (b <- data) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }

  def sha256(data: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-256").digest(data)

  object BLEBeacon {
    def hash(counter: ULong, key: String): String = {
      val counterByte: Array[Byte] =
        ((0 until (8 - counter.toByteArray.length)).map(_ => 0.asInstanceOf[Byte]) ++
          counter.toByteArray.toSeq).toArray
      val keyByte = key.getBytes("ASCII")
      val hash = sha256(counterByte ++ keyByte ++ counterByte)
      toHexString(hash.take(8))
    }

    def hash(counterStr: String, key: String): String =
      Crypto.parseULong(counterStr) match {
        case Some(counter) => hash(counter, key)
        case None => ""
      }

    def verify(receivedCounter: String, secretKey: String, receivedHash: String): Boolean =
      receivedCounter.length == 16 && receivedHash.length == 16 && hash(receivedCounter, secretKey) == receivedHash

    def verify(currentCounter: ULong, receivedCounter: String, secretKey: String, receivedHash: String): Option[ULong] = {
      val counter = parseULong(receivedCounter)
      counter match {
        case Some(value) if verify(receivedCounter, secretKey, receivedHash) && value > currentCounter => counter
        case _ => None
      }
    }

    def verifyDummy(receivedCounter: String, secretKey: String, receivedHash: String): Option[ULong] = {
      val counter = parseULong(receivedCounter)
      counter match {
        case Some(_) if verify(receivedCounter, secretKey, receivedHash) => counter
        case _ => None
      }
    }
  }

}
