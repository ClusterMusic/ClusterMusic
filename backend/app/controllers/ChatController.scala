package controllers

import chat._
import auth._

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.Timeout
import slick.jdbc.MySQLProfile.api._
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.scaladsl.Flow
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.model.Filters
import play.api.Configuration

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChatController @Inject()(
  val controllerComponents: ControllerComponents,
  userAction: UserAuthorizedAction,
  configuration: Configuration,
  mongoDatabase: MongoDatabase,
  database: Database,
  guardian: Guardian,
  @Named("blocking") blockingEc: ExecutionContext,    
  @Named("database") dbEc: ExecutionContext,          
  ec: ExecutionContext                                
)(implicit system: ActorSystem, mat: Materializer)
  extends BaseController {

  implicit val userActivationTimeout: Timeout = 3.seconds
  
  
  private val chatService: ActorRef = system.actorOf(
    Props(new ChatService(mongoDatabase)(blockingEc, mat)),
    "ChatService"
  )

  def chat(roomId: String): Action[AnyContent] = userAction.async { implicit request =>
    
    Future.successful(Ok(s"Chat room $roomId endpoint"))
  }

  def chatSocket: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    
    implicit val wsEc: ExecutionContext = blockingEc
    
    
    val authHeader = request.headers.get("Sec-WebSocket-Protocol").flatMap { protocols =>
      protocols.split(",").find(_.trim.startsWith("access_token.")).map(_.trim.substring(13))
    }.orElse(request.getQueryString("access_token"))
    
    val userIdOpt = request.getQueryString("user").map(_.toInt)
    
    (authHeader, userIdOpt) match {
      case (Some(token), Some(userId)) =>
        guardian.validateAccessToken(token) match {
          case Some(validUserId) if validUserId == userId.toString =>
            val user = model.Identity[model.User](userId)
            val flowFuture = (chatService ? CoupleUser(user)).mapTo[Flow[JsValue, JsValue, _]]
            flowFuture.map(flow => Right(flow))(blockingEc).recover {
              case ex =>
                Left(InternalServerError("WebSocket error: " + ex.getMessage))
            }(blockingEc)
          case _ =>
            Future.successful(Left(Unauthorized(Json.obj(
              "error" -> "Invalid or expired access token",
              "code" -> "TOKEN_INVALID"
            ))))
        }
      case _ =>
        Future.successful(Left(Unauthorized(Json.obj(
          "error" -> "Authorization required",
          "code" -> "NO_AUTH"
        ))))
    }
  }
}
