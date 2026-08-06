package chat

import model.{Identity, Post, User}
import play.api.libs.json.{JsObject, JsResult, JsValue, OFormat}
import play.api.libs.json.Json

import java.time.Instant
import java.util.UUID

sealed trait Message {
  var messageBlock: Option[UUID]
  val dateTime: Instant
}
object Message {

  private val chatMessageFormat: OFormat[ChatMessage] = Json.format[ChatMessage]
  private val postMessageFormat: OFormat[PostMessage] = Json.format[PostMessage]

  implicit val implMessageFormat: OFormat[Message] = new OFormat[Message] {
    override def writes(o: Message): JsObject = o match {
      case m: ChatMessage => 
        chatMessageFormat.writes(m) + ("mf" -> play.api.libs.json.JsBoolean(true))
      case m: PostMessage => 
        postMessageFormat.writes(m) + ("mf" -> play.api.libs.json.JsBoolean(false))
    }

    override def reads(json: JsValue): JsResult[Message] = {
      if ((json \ "mf").as[Boolean]) {
        chatMessageFormat.reads(json)
      } else {
        postMessageFormat.reads(json)
      }
    }
  }

  implicit val ordering: Ordering[Message] = Ordering[Instant].on(_.dateTime)
}
case class ChatMessage(
                        user: Identity[User],
                        chatMessage: String,
                        override val dateTime: Instant = Instant.now,
                        override var messageBlock: Option[UUID] = None
                      ) extends Message

case class PostMessage(
                        user: Identity[User],
                        post: Identity[Post],
                        override val dateTime: Instant = Instant.now,
                        override var messageBlock: Option[UUID] = None
                      ) extends Message

case class MessageBlock(_id: UUID, messages: Array[Message], roomId: Int, timestamp: Instant)

case class ChatRoomDocument(_id: Int, members: Seq[Identity[User]], blocks: Seq[UUID])

