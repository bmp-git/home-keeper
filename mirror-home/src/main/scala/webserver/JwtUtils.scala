package webserver

import java.time.Clock

import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json._
import webserver.model.JwtClaimData

import scala.concurrent.duration._
import scala.util.Try


object JwtUtils {
  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS512
  val expirationTime: FiniteDuration = 2000.days
  val secret = "secretKey"

  import webserver.model.ModelJsonProtocol._

  implicit val clock: Clock = Clock.systemUTC


  def generate(claim: JwtClaimData): String = Jwt.encode(
    JwtHeader(algorithm),
    JwtClaim(claim.toJson.toString).issuedNow.expiresIn(expirationTime.toSeconds),
    secret)

  def verify(token: String): Try[JwtClaimData] = Jwt.decode(
    token,
    secret,
    Seq(algorithm))
    .map(claim => JsonParser(ParserInput(claim.content)).convertTo[JwtClaimData])
}
