package auth

import model._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


case class Authorization(label: String) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case Authorization(label) => label == this.label
      case _ => false
    }
  }
}
object Authorization {
  val None: Authorization = Authorization("None")
  val User: Authorization = Authorization("User")
  val Admin: Authorization = Authorization("Admin")
}

case class AuthenticatedRequest[A](user: Identity[User], auth: Authorization, request: Request[A]) extends play.api.mvc.WrappedRequest[A](request)


class UserAuthorizedAction @Inject()(
  parser: BodyParsers.Default, 
  guardian: Guardian
)(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest, AnyContent]{

  private val logger = Logger(this.getClass)

  override def parser: BodyParser[AnyContent] = parser
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    logger.debug(s"authorizing ${request.method} ${request.path}")

    val userHeader = request.headers.get("User").getOrElse({
      logger.debug("denied: no User header")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "User header required",
        "code" -> "NO_USER_HEADER"
      )))
    })

    val userId = userHeader.toIntOption.getOrElse({
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "User header must be a numeric user id",
        "code" -> "INVALID_USER_HEADER"
      )))
    })

    val authHeader = request.headers.get("Authorization").getOrElse({
      logger.debug("denied: no Authorization header")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Authorization header required",
        "code" -> "NO_AUTH_HEADER"
      )))
    })

    if (!authHeader.startsWith("Bearer ")) {
      logger.debug("denied: malformed Authorization header")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid authorization header format",
        "code" -> "INVALID_AUTH_FORMAT"
      )))
    }
    val token = authHeader.substring(7)

    if (!guardian.validateAccessToken(token).contains(userHeader)) {
      logger.debug("denied: invalid or expired access token")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid or expired access token",
        "code" -> "TOKEN_INVALID"
      )))
    }

    val authenticatedRequest = AuthenticatedRequest(Identity[User](userId), Authorization.User, request)
    block(authenticatedRequest)
  }
}


class OptionalUserAction @Inject()(
  parser: BodyParsers.Default, 
  guardian: Guardian
)(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest, AnyContent]{
  
  override def parser: BodyParser[AnyContent] = parser
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val userHeader = request.headers.get("User").getOrElse({
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "User header required",
        "code" -> "NO_USER_HEADER"
      )))
    })

    val userId = userHeader.toIntOption.getOrElse({
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "User header must be a numeric user id",
        "code" -> "INVALID_USER_HEADER"
      )))
    })

    val authHeader = request.headers.get("Authorization").getOrElse({
      val unauthenticatedRequest = AuthenticatedRequest(Identity[User](userId), Authorization.None, request)
      return block(unauthenticatedRequest)
    })

    if(!authHeader.startsWith("Bearer ")){
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid or expired access token",
        "code" -> "TOKEN_INVALID"
      )))
    }
    val token = authHeader.substring(7)

    
    if (!guardian.validateAccessToken(token).contains(userHeader)) {
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid or expired access token",
        "code" -> "TOKEN_INVALID"
      )))
    }

    val authenticatedRequest = AuthenticatedRequest(Identity[User](userId), Authorization.User, request)
    block(authenticatedRequest)
  }
}

