package webserver

import java.time.Clock

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}
import config.ConfigDsl
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json._
import utils.File
import webserver.json.JwtClaimData

import scala.concurrent.duration._
import scala.util.Try

object JwtUtils {
  val secretKeyPath: String = ConfigDsl.RESOURCE_FOLDER + "/secret_key"
  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS512
  val secret: String = File.read(secretKeyPath).getOrElse("secret_key")

  import webserver.json.JsonModel._

  implicit val clock: Clock = Clock.systemUTC

  def generate(claim: JwtClaimData, expirationTime: FiniteDuration): String = Jwt.encode(
    JwtHeader(algorithm),
    JwtClaim(claim.toJson.toString).issuedNow.expiresIn(expirationTime.toSeconds),
    secret)

  def verify(token: String): Try[JwtClaimData] = Jwt.decode(
    token,
    secret,
    Seq(algorithm))
    .map(claim => JsonParser(ParserInput(claim.content)).convertTo[JwtClaimData])

  def tokenAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case Credentials.Provided(token) => JwtUtils.verify(token).map(_.user).toOption
      case _ => None
    }

  def secured: AuthenticationDirective[String] = authenticateOAuth2(realm = "secure mirror home", tokenAuthenticator)

  def unsecured: AuthenticationDirective[String] = authenticateOAuth2(realm = "secure mirror home", _ => Some("anonymous"))
}
