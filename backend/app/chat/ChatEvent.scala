package chat
import model.{Identity, User}
import play.api.libs.json.{JsObject, JsString, Json, OWrites, Writes, OFormat}

import java.net.URL
import java.time.Instant
import java.util.UUID

sealed trait ChatEvent

object ChatEvent {

  
  case class MessageEvent(message: Message, chatId: Int) extends ChatEvent
  case class UserRemoved(user: Identity[User], chatId: Int) extends ChatEvent
  case class UserAdded(user: Identity[User], chatId: Int) extends ChatEvent
  case class UserSubscribed(user: Identity[User], chatId: Int) extends ChatEvent
  case class UserUnsubscribed(user: Identity[User], chatId: Int) extends ChatEvent
  case class UserJoined(user: Identity[User], chatId: Int) extends ChatEvent
  case class UserLeft(user: Identity[User], chatId: Int) extends ChatEvent

  case class ResponseMessages(messages: Seq[Message]) extends ChatEvent
  case class ResponseSnapshot(snapshot: ChatRoom) extends ChatEvent
  case class ResponseRooms(rooms: Seq[Int]) extends ChatEvent
  case class ResponseFailed(reason: String) extends ChatEvent
  case class ResponseActionSuccess() extends ChatEvent

  

  private def addType[T](typ: String)(implicit ow: OWrites[T]): OWrites[T] =
    OWrites[T](t => ow.writes(t) + ("typ" -> JsString(typ)))

  
  private implicit val messageEventJson: OWrites[MessageEvent] = Json.writes[MessageEvent]
  private implicit val userRemovedJson: OWrites[UserRemoved] = Json.writes[UserRemoved]
  private implicit val userAddedJson: OWrites[UserAdded] = Json.writes[UserAdded]
  private implicit val userSubscribedJson: OWrites[UserSubscribed] = Json.writes[UserSubscribed]
  private implicit val userUnsubscribedJson: OWrites[UserUnsubscribed] = Json.writes[UserUnsubscribed]
  private implicit val userJoinedJson: OWrites[UserJoined] = Json.writes[UserJoined]
  private implicit val userLeftJson: OWrites[UserLeft] = Json.writes[UserLeft]
  private implicit val responseMessagesJson: OWrites[ResponseMessages] = Json.writes[ResponseMessages]
  private implicit val responseSnapshotJson: OWrites[ResponseSnapshot] = Json.writes[ResponseSnapshot]
  private implicit val responseRoomsJson: OWrites[ResponseRooms] = Json.writes[ResponseRooms]
  private implicit val responseFailedJson: OWrites[ResponseFailed] = Json.writes[ResponseFailed]
  private implicit val responseSuccessJson: OWrites[ResponseActionSuccess] = OWrites(_ => Json.obj())

  
  private val messageEventWrites        = addType[MessageEvent]("userMessage")
  private val userRemovedEventWrites    = addType[UserRemoved]("userRemoved")
  private val userAddedEventWrites      = addType[UserAdded]("userAdded")
  private val userSubscribedEventWrites = addType[UserSubscribed]("userSubscribed")
  private val userUnsubscribedEventWrites = addType[UserUnsubscribed]("userUnsubscribed")
  private val userJoinedEventWrites     = addType[UserJoined]("userJoined")
  private val userLeftEventWrites       = addType[UserLeft]("userLeft")
  private val responseMessageBlockWrites = addType[ResponseMessages]("responseMessages")
  private val responseRoomsWrites        = addType[ResponseRooms]("responseRooms")
  private val responseSnapshotWrites    = addType[ResponseSnapshot]("responseSnapshot")
  private val responseFailedWrites      = addType[ResponseFailed]("responseFailed")
  private val responseActionSuccessWrites = addType[ResponseActionSuccess]("responseActionSuccess")

  
  private implicit val chatRoomWrites: OWrites[ChatRoom] = Json.writes[ChatRoom]

  implicit val implChatEventWrites: OWrites[ChatEvent] = {
    case e: MessageEvent          => messageEventWrites.writes(e)
    case e: UserRemoved           => userRemovedEventWrites.writes(e)
    case e: UserAdded             => userAddedEventWrites.writes(e)
    case e: UserSubscribed        => userSubscribedEventWrites.writes(e)
    case e: UserUnsubscribed      => userUnsubscribedEventWrites.writes(e)
    case e: UserJoined            => userJoinedEventWrites.writes(e)
    case e: UserLeft              => userLeftEventWrites.writes(e)
    case r: ResponseMessages      => responseMessageBlockWrites.writes(r)
    case r: ResponseRooms         => responseRoomsWrites.writes(r)
    case r: ResponseSnapshot      => responseSnapshotWrites.writes(r)
    case r: ResponseFailed        => responseFailedWrites.writes(r)
    case r: ResponseActionSuccess => responseActionSuccessWrites.writes(r)
  }
}



