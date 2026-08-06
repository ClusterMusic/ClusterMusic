package auth


/* 
Guardian validates and issues access tokens.
Implementations configure the guardian
 */
import model._
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

import java.time.Clock
import java.security.{KeyPair, PrivateKey, PublicKey}
import java.time.Instant
import java.time.temporal.ChronoUnit
import pdi.jwt.{Jwt, JwtClaim}

trait Guardian {
  implicit val clock: Clock = Clock.systemUTC()

  val accessTokenValidity: Long
  val accessTokenUnit: ChronoUnit

  val refreshTokenValidity: Long
  val refreshTokenUnit: ChronoUnit

  val algorithm: JwtAsymmetricAlgorithm
  val keyPair: KeyPair
  val privateKey: PrivateKey
  val publicKey: PublicKey

  val claimIssuer: String

  def validateAccessToken(token: String): Option[String] = {
    Jwt.decode(token, publicKey, Seq(algorithm)).toOption.filter{
        decoded => decoded.isValid(clock)
      }
      .flatMap{
        _.subject
      }
  }

  def giveAccessToken(identity: Identity[User]): String = {
    val expiry = Instant.now().plus(accessTokenValidity, accessTokenUnit).getEpochSecond
    val claim = JwtClaim(
      subject = Some(identity.value.toString),
      issuer = Some(claimIssuer),
      issuedAt = Some(Instant.now().getEpochSecond),
      expiration = Some(expiry)
    )
    Jwt.encode(claim, privateKey, algorithm)
  }
}
