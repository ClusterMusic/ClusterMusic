package auth

import model.{Identity, User}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.security.{KeyPair, KeyPairGenerator}
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}

class GuardianSpec extends AnyWordSpec with Matchers {

  private def rsaKeyPair(): KeyPair = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    kpg.genKeyPair()
  }

  private class TestGuardian(
    keys: KeyPair,
    at: Clock = Clock.systemUTC(),
    validity: Long = 1,
    unit: ChronoUnit = ChronoUnit.HOURS
  ) extends Guardian {
    override implicit val clock: Clock = at
    override val accessTokenValidity: Long = validity
    override val accessTokenUnit: ChronoUnit = unit
    override val refreshTokenValidity: Long = 10
    override val refreshTokenUnit: ChronoUnit = ChronoUnit.DAYS
    override val algorithm: JwtAsymmetricAlgorithm = JwtAlgorithm.RS256
    override val keyPair: KeyPair = keys
    override val privateKey = keys.getPrivate
    override val publicKey = keys.getPublic
    override val claimIssuer: String = "ClusterApp"
  }

  private val alice = Identity[User](42)

  "giveAccessToken / validateAccessToken" should {

    "round-trip a freshly issued token back to its user id" in {
      val guardian = new TestGuardian(rsaKeyPair())
      val token = guardian.giveAccessToken(alice)
      guardian.validateAccessToken(token) shouldBe Some("42")
    }

    "issue a different token for a different user" in {
      val guardian = new TestGuardian(rsaKeyPair())
      val forAlice = guardian.giveAccessToken(alice)
      val forBob = guardian.giveAccessToken(Identity[User](43))
      forAlice should not equal forBob
      guardian.validateAccessToken(forBob) shouldBe Some("43")
    }

    "reject a token signed by a different key pair" in {
      val ours = new TestGuardian(rsaKeyPair())
      val attacker = new TestGuardian(rsaKeyPair())
      val forged = attacker.giveAccessToken(alice)
      ours.validateAccessToken(forged) shouldBe None
    }

    "reject a token whose payload has been tampered with" in {
      val guardian = new TestGuardian(rsaKeyPair())
      val token = guardian.giveAccessToken(alice)
      val Array(header, payload, signature) = token.split('.')
      val escalated = java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(
        new String(java.util.Base64.getUrlDecoder.decode(payload))
          .replace("\"42\"", "\"1\"")
          .getBytes("UTF-8")
      )
      guardian.validateAccessToken(s"$header.$escalated.$signature") shouldBe None
    }

    "reject a token that has expired" in {
      val keys = rsaKeyPair()
      val issuer = new TestGuardian(keys, validity = 1, unit = ChronoUnit.SECONDS)
      val token = issuer.giveAccessToken(alice)

      val later = Clock.fixed(Instant.now().plusSeconds(60), ZoneOffset.UTC)
      val verifier = new TestGuardian(keys, at = later)
      verifier.validateAccessToken(token) shouldBe None
    }

    "accept a token that has not expired yet" in {
      val keys = rsaKeyPair()
      val issuer = new TestGuardian(keys, validity = 1, unit = ChronoUnit.HOURS)
      val token = issuer.giveAccessToken(alice)

      val soon = Clock.fixed(Instant.now().plusSeconds(60), ZoneOffset.UTC)
      new TestGuardian(keys, at = soon).validateAccessToken(token) shouldBe Some("42")
    }

    "reject a token with no subject claim" in {
      val keys = rsaKeyPair()
      val guardian = new TestGuardian(keys)
      val subjectless = Jwt.encode(
        JwtClaim(
          issuer = Some("ClusterApp"),
          issuedAt = Some(Instant.now().getEpochSecond),
          expiration = Some(Instant.now().plusSeconds(3600).getEpochSecond)
        ),
        keys.getPrivate,
        JwtAlgorithm.RS256
      )
      guardian.validateAccessToken(subjectless) shouldBe None
    }

    "reject structurally invalid tokens rather than throwing" in {
      val guardian = new TestGuardian(rsaKeyPair())
      val junk = Seq("", "   ", "not-a-jwt", "a.b", "a.b.c", "....", "Bearer abc")
      junk.foreach { t =>
        withClue(s"token '$t': ") { guardian.validateAccessToken(t) shouldBe None }
      }
    }

    "reject an unsigned 'alg: none' token" in {
      // JWT downgrade attack.
      val guardian = new TestGuardian(rsaKeyPair())
      val unsigned = Jwt.encode(
        JwtClaim(
          subject = Some("42"),
          issuer = Some("ClusterApp"),
          expiration = Some(Instant.now().plusSeconds(3600).getEpochSecond)
        )
      )
      guardian.validateAccessToken(unsigned) shouldBe None
    }
  }

  "ClusterGuardian" should {
    "issue tokens that it accepts back" in {
      val token = ClusterGuardian.giveAccessToken(alice)
      ClusterGuardian.validateAccessToken(token) shouldBe Some("42")
    }

    "reject a token minted by an unrelated key pair" in {
      val attacker = new TestGuardian(rsaKeyPair())
      ClusterGuardian.validateAccessToken(attacker.giveAccessToken(alice)) shouldBe None
    }
  }

  "BadGuardian" should {
    "accept any arbitrary string as a valid token" in {
      BadGuardian.validateAccessToken("completely-made-up") shouldBe Some("completely-made-up")
    }

    "let an attacker authenticate as any user they name" in {
      BadGuardian.validateAccessToken("1") shouldBe Some("1")
    }
  }
}
