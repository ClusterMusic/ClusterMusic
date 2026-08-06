package chat

import model._
import org.apache.pekko.actor.{Actor, ActorRef}
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt


class UserActor(user: Identity[User], out: SourceQueueWithComplete[JsValue], chatService: ActorRef)(implicit ec: ExecutionContext) extends Actor {

  var optChat: Option[ActorRef] = None
  var subscriptions: Set[ActorRef] = Set.empty

  implicit val timeout: Timeout = 3.seconds

  override def postStop(): Unit = {
    optChat.foreach(_ ! LeaveRoom(user))
    subscriptions.foreach(_ ! Unsubscribe())
  }

  override def receive: Receive = {
    case UserAction.JoinRoom(roomId) =>
      (chatService ? GetRoom(roomId)).map {
        case RoomRef(room: ActorRef, _) =>
          println("Joining room:" + room)
          room ! JoinRoom(user)
          optChat = Some(room)

        case RoomNotAvailable(reason) =>
          out.offer(Json.toJson(
            ChatEvent.ResponseFailed(reason).asInstanceOf[ChatEvent]
          ))
      }

    case ClosedRoom() =>
      optChat = None

    case OpenedRoom(room) =>
      print("Opened room:" + room)
      room ! Subscribe(user)
      subscriptions += room

    
    case ActionSuccess() =>
      
      println(s"[UserActor] Action completed successfully for user ${user.value}")

    case ActionFailed(reason) =>
      
      out.offer(Json.toJson(
        ChatEvent.ResponseFailed(reason).asInstanceOf[ChatEvent]
      ))

    case chatEvent: ChatEvent =>
      out.offer(Json.toJson(chatEvent))


    case action: UserAction => action match {
      case UserAction.GetRooms() =>
        out.offer(Json.toJson(
          ChatEvent.ResponseRooms(subscriptions.toSeq.map(_.hashCode())).asInstanceOf[ChatEvent]
        ))
        
      case UserAction.Exit() =>
        context.stop(self)

      case _ => optChat match {
        case Some(chat) =>
          (chat ? (action, user)).mapTo[ChatEvent].recover {
            case ex => ChatEvent.ResponseFailed(s"Unable to complete action: ${ex.getMessage}")
          }.map(x => out.offer(Json.toJson(x)))
        case None =>
          out.offer(Json.toJson(
            ChatEvent.ResponseFailed("Unable to complete action without viewing a chat").asInstanceOf[ChatEvent]
          ))
      }
    }

    case unknown =>
      println(s"ChatUserActor received unknown message: $unknown")
  }
}
