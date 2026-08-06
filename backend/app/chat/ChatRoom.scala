package chat

import model._
import org.apache.pekko.actor.{Actor, ActorRef}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout

import java.time.Instant
import scala.collection.{SortedSet, mutable}
import scala.collection.immutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class ChatRoom(id: Int, members: Seq[Identity[User]], messages: Seq[Message])

case class ChatRoomMemo(title: String, recentMessage: Message)

class ChatRoomActor(chatRoom: ChatRoom, chatService: ActorRef)(implicit ec: ExecutionContext) extends Actor {

  implicit val timeout: Timeout = 3.seconds
  private val MESSAGE_LIMIT = 30

  private var lastTakenMessageTimeStamp = chatRoom.messages.headOption.map(_.dateTime)

  private var members: Set[Identity[User]] = chatRoom.members.toSet
  private val messages: ArrayBuffer[Message] = ArrayBuffer(chatRoom.messages: _*)



  private var viewers: Set[ActorRef] = Set.empty
  private var subscribers: Set[ActorRef] = Set.empty

  private def broadcast(message: Any): Unit = {
    subscribers.foreach(_ ! message)
  }


  override def postStop(): Unit = {
    viewers.foreach(_ ! ClosedRoom)
    chatService ! WriteSnapshot(ChatRoom(chatRoom.id, members.toSeq, messages.toSeq))
  }

  override def preStart(): Unit = {
    chatService ! AnnounceRoom(members)
  }


  override def receive: Receive = {
    case Subscribe(user) =>
      val rec = sender()
      if(!members.contains(user)){
        rec ! ActionFailed("Cannot subscribe to room that user is not member of!")
      } else {
        subscribers += rec
        rec ! ActionSuccess()
      }

    case Unsubscribe() =>
      val rec = sender()
      subscribers -= rec
      viewers -= rec
      if (subscribers.isEmpty) {
        chatService ! RemoveRoom(chatRoom.id)
      }

    case JoinRoom(user) =>
      val rec = sender()
      println(s"Entering user ${user.value}")
      if(!subscribers.contains(rec)){
        rec ! ActionFailed("Cannot view room that user is not subscribed to!")
      }else{
        viewers += rec
        broadcast(ChatEvent.UserJoined(user, chatRoom.id))
      }


    case LeaveRoom(user) =>
      val rec = sender()
      println(s"Leaving user ${user.value}")
      viewers -= rec

      if(viewers.isEmpty) {
        chatService ! RemoveRoom(chatRoom.id)
      } else {
        broadcast(ChatEvent.UserLeft(user, chatRoom.id))
      }



    case UserAction.GetSnapshot() =>
      sender() ! ChatEvent.ResponseSnapshot(ChatRoom(chatRoom.id, members.toSeq, messages.toSeq))

    case (action: UserAction, user: Identity[User]) => action match {
      case UserAction.SendChat(messageStr: String) =>
        val message = ChatMessage(user, messageStr)
        messages += message
        broadcast(ChatEvent.MessageEvent(message, chatRoom.id))

      case UserAction.SendPost(post: Identity[Post]) =>
        val message = PostMessage(user, post)
        messages += message
        broadcast(ChatEvent.MessageEvent(message, chatRoom.id))

      case UserAction.GetMessages(timestamp: Instant, _limit: Int) =>
        val rec = sender()
        val limit = if (_limit < MESSAGE_LIMIT) _limit else MESSAGE_LIMIT
        val response = messages.reverse.take(limit).takeWhile(_.dateTime.isBefore(timestamp))
        if (response.size < limit) {
          (chatService ? GetMessageBlock(chatRoom.id, Some(timestamp))).map {
            case mb: MessageBlock =>
              rec ! ChatEvent.ResponseMessages(mb.messages.appendedAll(response.toArray))
            case MessagesNotAvailable(_) =>
              rec ! ChatEvent.ResponseMessages(response.toSeq)
          }
        } else {
          rec ! ChatEvent.ResponseMessages(response.toSeq)
        }
    }

    case OpenRoomToMember(user: Identity[User], userActor: ActorRef) =>
      if(members.contains(user)){
        userActor ! OpenedRoom(self)
      }

    case SaveSnapshot() =>
      chatService ! WriteSnapshot(ChatRoom(chatRoom.id, members.toSeq, messages.toSeq))
  }
}