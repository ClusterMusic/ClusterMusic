package auth

import model._
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

  override def parser: BodyParser[AnyContent] = parser
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    println("User Authorized Action:")
    println("Headers: " + request.headers.toString())
    println("Body: " + request.body.toString())
    println("Method: " + request.method)
    
    val userHeader = request.headers.get("User").getOrElse({
      println("No user id found. Denied")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "User header required",
        "code" -> "NO_USER_HEADER"
      )))
    })

    val authHeader = request.headers.get("Authorization").getOrElse({
      println("No access token found. Denied")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Authorization header required",
        "code" -> "NO_AUTH_HEADER"
      )))
    })

    if (!authHeader.startsWith("Bearer ")) {
      println("Invalid authorization header format. Denied")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid authorization header format",
        "code" -> "INVALID_AUTH_FORMAT"
      )))
    }
    val token = authHeader.substring(7)

    println("UserId: " + userHeader)

    if (!guardian.validateAccessToken(token).contains(userHeader)) {
      println("Invalid access token. Denied")
      return Future.successful(Results.Unauthorized(Json.obj(
        "error" -> "Invalid or expired access token",
        "code" -> "TOKEN_INVALID"
      )))
    }

    println("Valid access token. Allowed")

    val authenticatedRequest = AuthenticatedRequest(Identity[User](userHeader.toInt), Authorization.User, request)
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

    val authHeader = request.headers.get("Authorization").getOrElse({
      val unauthenticatedRequest = AuthenticatedRequest(Identity[User](userHeader.toInt), Authorization.None, request)
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

    val authenticatedRequest = AuthenticatedRequest(Identity[User](userHeader.toInt), Authorization.User, request)
    block(authenticatedRequest)
  }
}

