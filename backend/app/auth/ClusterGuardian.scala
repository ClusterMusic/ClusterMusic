package auth

/* 
Implementation of Guardian for production use.
 */

import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

import java.security.{KeyPair, KeyPairGenerator, PrivateKey, PublicKey}
import java.time.temporal.ChronoUnit

object ClusterGuardian extends Guardian {

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
}
