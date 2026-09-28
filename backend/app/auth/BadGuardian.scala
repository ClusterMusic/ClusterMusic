package auth

import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import model.{Identity, User}

import java.security.{KeyPair, KeyPairGenerator, PrivateKey, PublicKey}
import java.time.Instant
import java.time.temporal.ChronoUnit

object BadGuardian extends Guardian {
  override val refreshTokenValidity: Long = 10
  override val refreshTokenUnit: ChronoUnit = ChronoUnit.DAYS
  override val algorithm: JwtAsymmetricAlgorithm = JwtAlgorithm.RS256
  override val keyPair: KeyPair = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048) 
    kpg.genKeyPair()
  }
  override val privateKey: PrivateKey = keyPair.getPrivate
  override val publicKey: PublicKey = keyPair.getPublic

  override val claimIssuer: String = "ClusterApp"
  override val accessTokenValidity: Long = 1
  override val accessTokenUnit: ChronoUnit = ChronoUnit.HOURS


  override def validateAccessToken(token: String): Option[String] = {
    Some(token)
  }

  override def giveAccessToken(identity: Identity[User]): String = {
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
