package controllers

import model._
import auth._
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json.{JsError, JsValue, Json, Reads, Writes}
import play.api.mvc.{Action, BaseController, ControllerComponents, AnyContent}

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}
import javax.inject.{Inject, Named}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

class AuthController @Inject()(
  val controllerComponents: ControllerComponents,
  implicit val database: Database,
  guardian: Guardian,
  @Named("database") dbEc: ExecutionContext,  
  ec: ExecutionContext
) extends BaseController {  

  case class LoginRequest(username: String, password: String)
  case class RefreshRequest(userid: Int, refreshToken: String)
  case class LogoutRequest(userid: Int, refreshToken: String)
  case class RegisterRequest(username: String, password: String, biography: String, communityId: Int)

  case class AuthResponse(accessToken: String,
                          refreshToken: String,
                          tokenType: String = "Bearer",
                          expiresIn: Long,
                          userId: Int)
  case class TokenValidationResponse(valid: Boolean,
                                     userId: Option[Int] = None,
                                     message: String)



  implicit val loginRequestReads: Reads[LoginRequest] = Json.reads[LoginRequest]
  implicit val refreshRequestReads: Reads[RefreshRequest] = Json.reads[RefreshRequest]
  implicit val logoutRequestReads: Reads[LogoutRequest] = Json.reads[LogoutRequest]
  implicit val registerRequestReads: Reads[RegisterRequest] = Json.reads[RegisterRequest]
  implicit val authResponseWrites: Writes[AuthResponse] = Json.writes[AuthResponse]
  implicit val tokenValidationResponseWrites: Writes[TokenValidationResponse] = Json.writes[TokenValidationResponse]

  
  val refreshTokens = TrieMap[Int, RefreshToken]()

  def loginPage: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login())
  }

  def registerPage: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.register())
  }

  def register: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[RegisterRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format"))),
      registerReq => {
        println("Registering user: " + registerReq.username)
        implicit val databaseEc: ExecutionContext = dbEc
        
        
        val checkQuery = User.table.filter(_.username === registerReq.username).result.headOption
        
        database.run(checkQuery).flatMap {
          case Some(_) =>
            Future.successful(BadRequest(Json.obj("error" -> "Username already exists")))
          case None =>
            println("Community ID: " + registerReq.communityId)
            
            val newUser = UserConnection(
              id = Identity[User](0),
              username = registerReq.username,
              hashedKey = registerReq.password,
              name = registerReq.username,
              image = "",
              biography = registerReq.biography,
              community = Identity[Community](registerReq.communityId),
              createdAt = Timestamp.valueOf(LocalDateTime.now()),
              rank = 0.0,
              score = 0.0
            )
            
            val insertAction = (User.table returning User.table.map(_.id)) += newUser

            
            database.run(insertAction).map { userId =>

              val accessToken = guardian.giveAccessToken(userId)
              val refreshToken = RefreshToken(
                token = java.util.UUID.randomUUID().toString,
                userId = userId.value,
                expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)
              )
              refreshTokens += userId.value -> refreshToken
              Ok(Json.toJson(AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken.token,
                expiresIn = 3600,
                userId = userId.value
              )))
            }(dbEc).recover {
              case e: Exception =>
                println("Exception during user creation: " + e.toString)
                InternalServerError(Json.obj("error" -> "Failed to create user"))
            }(dbEc)
        }(dbEc)
      }
    )
  }

  def login: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format"))),
      loginReq => {
        
        implicit val databaseEc: ExecutionContext = dbEc
        
        
        val query = User.table.filter(u => u.username === loginReq.username && u.hashedKey === loginReq.password).result.headOption
        
        database.run(query).map {
          case Some(user) =>
            
            val userId = user.id.value
            val accessToken = guardian.giveAccessToken(user.id)
            val refreshToken = RefreshToken(
              token = java.util.UUID.randomUUID().toString,
              userId = userId,
              expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60) 
            )
            
            refreshTokens += userId -> refreshToken
            
            Ok(Json.toJson(AuthResponse(
              accessToken = accessToken,
              refreshToken = refreshToken.token,
              expiresIn = 3600,
              userId = userId
            )))
            
          case None =>
            Unauthorized(Json.obj("error" -> "Invalid credentials"))
        }(ec)
      }
    )
  }

  def refresh: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[RefreshRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format"))),
      refreshReq => {
        refreshTokens.get(refreshReq.userid) match {
          case Some(storedToken) if storedToken.token == refreshReq.refreshToken && storedToken.expiresAt.isAfter(Instant.now()) =>
            
            val newAccessToken = guardian.giveAccessToken(Identity[User](refreshReq.userid))
            val newRefreshToken = RefreshToken(
              token = java.util.UUID.randomUUID().toString,
              userId = refreshReq.userid,
              expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)
            )
            
            refreshTokens += refreshReq.userid -> newRefreshToken
            
            Future.successful(Ok(Json.toJson(AuthResponse(
              accessToken = newAccessToken,
              refreshToken = newRefreshToken.token,
              expiresIn = 3600,
              userId = refreshReq.userid
            ))))
            
          case _ =>
            Future.successful(Unauthorized(Json.obj("error" -> "Invalid or expired refresh token")))
        }
      }
    )
  }

  def logout: Action[JsValue] = Action(parse.json) { implicit request =>
    request.body.validate[LogoutRequest].fold(
      errors => BadRequest(Json.obj("error" -> "Invalid request format")),
      logoutReq => {
        refreshTokens -= logoutReq.userid
        Ok(Json.obj("message" -> "Logged out successfully"))
      }
    )
  }

  def validateToken: Action[AnyContent] = Action { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        guardian.validateAccessToken(token) match {
          case Some(userId) =>
            Ok(Json.toJson(TokenValidationResponse(
              valid = true,
              userId = Some(userId.toInt),
              message = "Token is valid"
            )))
          case None =>
            Ok(Json.toJson(TokenValidationResponse(
              valid = false,
              message = "Token is invalid or expired"
            )))
        }
      case _ =>
        BadRequest(Json.obj("error" -> "Authorization header required"))
    }
  }
}