package auth

import model.{Identity, User}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.prop.TableDrivenPropertyChecks
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.security.{KeyPair, KeyPairGenerator}
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class AuthorizationSpec
    extends AnyWordSpec
    with Matchers
    with TableDrivenPropertyChecks
    with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem("authorization-spec")
  private implicit val mat: Materializer = Materializer(system)
  private implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  private object StubGuardian extends Guardian {
    override val accessTokenValidity: Long = 1
    override val accessTokenUnit: ChronoUnit = ChronoUnit.HOURS
    override val refreshTokenValidity: Long = 10
    override val refreshTokenUnit: ChronoUnit = ChronoUnit.DAYS
    override val algorithm: JwtAsymmetricAlgorithm = JwtAlgorithm.RS256
    override val keyPair: KeyPair = {
      val kpg = KeyPairGenerator.getInstance("RSA")
      kpg.initialize(2048)
      kpg.genKeyPair()
    }
    override val privateKey = keyPair.getPrivate
    override val publicKey = keyPair.getPublic
    override val claimIssuer: String = "ClusterApp"

    override def validateAccessToken(token: String): Option[String] =
      if (token.startsWith("valid-")) Some(token.stripPrefix("valid-")) else None
  }

  private val parser = new BodyParsers.Default(stubPlayBodyParsers)
  private val requireAuth = new UserAuthorizedAction(parser, StubGuardian)
  private val optionalAuth = new OptionalUserAction(parser, StubGuardian)

  private def run(
    builder: play.api.mvc.ActionBuilder[AuthenticatedRequest, play.api.mvc.AnyContent],
    headers: Seq[(String, String)]
  ): Future[Result] = {
    val action = builder { request =>
      Results.Ok(s"${request.user.value}:${request.auth.label}")
    }
    call(action, FakeRequest("GET", "/").withHeaders(headers: _*))
  }

  private def codeOf(result: Future[Result]): String =
    (contentAsJson(result) \ "code").asOpt[String].getOrElse("")

  private val noHeaders = Seq.empty[(String, String)]

  "UserAuthorizedAction" should {

    val cases = Table(
      ("scenario", "headers", "status", "code"),
      ("no headers at all",
        noHeaders, UNAUTHORIZED, "NO_USER_HEADER"),
      ("a token but no User header",
        Seq("Authorization" -> "Bearer valid-1"), UNAUTHORIZED, "NO_USER_HEADER"),
      ("a User header but no token",
        Seq("User" -> "1"), UNAUTHORIZED, "NO_AUTH_HEADER"),
      ("a token missing the Bearer prefix",
        Seq("User" -> "1", "Authorization" -> "valid-1"), UNAUTHORIZED, "INVALID_AUTH_FORMAT"),
      ("a lowercase bearer prefix",
        Seq("User" -> "1", "Authorization" -> "bearer valid-1"), UNAUTHORIZED, "INVALID_AUTH_FORMAT"),
      ("a token the guardian rejects",
        Seq("User" -> "1", "Authorization" -> "Bearer forged"), UNAUTHORIZED, "TOKEN_INVALID"),
      ("an empty bearer token",
        Seq("User" -> "1", "Authorization" -> "Bearer "), UNAUTHORIZED, "TOKEN_INVALID"),
      ("a valid token belonging to a different user",
        Seq("User" -> "1", "Authorization" -> "Bearer valid-2"), UNAUTHORIZED, "TOKEN_INVALID")
    )

    "reject every unauthenticated request shape" in {
      forAll(cases) { (scenario: String, headers: Seq[(String, String)], expected: Int, code: String) =>
        withClue(s"$scenario: ") {
          val result = run(requireAuth, headers)
          status(result) shouldBe expected
          codeOf(result) shouldBe code
        }
      }
    }

    "admit a request whose token matches its User header" in {
      val result = run(requireAuth, Seq("User" -> "7", "Authorization" -> "Bearer valid-7"))
      status(result) shouldBe OK
      contentAsString(result) shouldBe "7:User"
    }

    "never grant Admin authorization" in {
      val result = run(requireAuth, Seq("User" -> "1", "Authorization" -> "Bearer valid-1"))
      contentAsString(result) should not include Authorization.Admin.label
    }

    "reject a non-numeric User header instead of throwing" in {
      val result = run(requireAuth, Seq("User" -> "abc", "Authorization" -> "Bearer valid-abc"))
      status(result) shouldBe UNAUTHORIZED
      codeOf(result) shouldBe "INVALID_USER_HEADER"
    }
  }

  "OptionalUserAction" should {

    "still require a User header" in {
      val result = run(optionalAuth, noHeaders)
      status(result) shouldBe UNAUTHORIZED
      codeOf(result) shouldBe "NO_USER_HEADER"
    }

    "admit an anonymous request with Authorization.None" in {
      val result = run(optionalAuth, Seq("User" -> "5"))
      status(result) shouldBe OK
      contentAsString(result) shouldBe "5:None"
    }

    "upgrade a request with a valid token to Authorization.User" in {
      val result = run(optionalAuth, Seq("User" -> "5", "Authorization" -> "Bearer valid-5"))
      status(result) shouldBe OK
      contentAsString(result) shouldBe "5:User"
    }

    val rejected = Table(
      ("scenario", "headers"),
      ("a forged token", Seq("User" -> "5", "Authorization" -> "Bearer forged")),
      ("a malformed header", Seq("User" -> "5", "Authorization" -> "valid-5")),
      ("a token for another user", Seq("User" -> "5", "Authorization" -> "Bearer valid-6"))
    )

    "reject a present-but-bad token rather than downgrading to anonymous" in {
      forAll(rejected) { (scenario: String, headers: Seq[(String, String)]) =>
        withClue(s"$scenario: ") {
          val result = run(optionalAuth, headers)
          status(result) shouldBe UNAUTHORIZED
          codeOf(result) shouldBe "TOKEN_INVALID"
        }
      }
    }

    "reject a non-numeric User header instead of throwing" in {
      val result = run(optionalAuth, Seq("User" -> "abc"))
      status(result) shouldBe UNAUTHORIZED
      codeOf(result) shouldBe "INVALID_USER_HEADER"
    }
  }

  "Authorization levels" should {
    "be distinct from one another" in {
      Set(Authorization.None, Authorization.User, Authorization.Admin) should have size 3
    }

    "compare by label" in {
      Authorization("User") shouldBe Authorization.User
      Authorization.User should not be Authorization.Admin
    }
  }
}
