package chat

import org.mongodb.scala.{Document, MongoCollection, MongoDatabase}
import org.mongodb.scala.model.{Filters, Indexes, Sorts, Updates}
import model._
import slick.jdbc.MySQLProfile.api.Database
import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, Props}
import org.apache.pekko.pattern.{ask, pipe}
import org.apache.pekko.stream.{Materializer, OverflowStrategy, ThrottleMode}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{Format, JsValue, Json, OFormat, Reads, Writes}
import UserAction.implUserActionReads

import java.time.Instant
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class ChatService(chatdb: MongoDatabase)(implicit ec: ExecutionContext, mat: Materializer) extends Actor {


  implicit val timeout: Timeout = 3.seconds
  private val saveScheduler: Cancellable =
    context.system.scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes, self, SaveRooms())

  
  implicit val identityUserReads: Reads[Identity[User]] = Reads[Identity[User]] { json =>
    (json \ "value").validate[Int].map(value => Identity[User](value))
  }
  
  implicit val identityUserWrites: Writes[Identity[User]] = Writes[Identity[User]] { identity =>
    Json.obj("value" -> identity.value)
  }
  
  implicit val identityUserFormat: Format[Identity[User]] = Format(identityUserReads, identityUserWrites)
  
  implicit val identityPostReads: Reads[Identity[Post]] = Reads[Identity[Post]] { json =>
    (json \ "value").validate[Int].map(value => Identity[Post](value))
  }
  
  implicit val identityPostWrites: Writes[Identity[Post]] = Writes[Identity[Post]] { identity =>
    Json.obj("value" -> identity.value)
  }
  
  implicit val identityPostFormat: Format[Identity[Post]] = Format(identityPostReads, identityPostWrites)
  
  implicit val chatMessageFormat: OFormat[ChatMessage] = Json.format[ChatMessage]
  implicit val postMessageFormat: OFormat[PostMessage] = Json.format[PostMessage]
  
  implicit val messageFormat: OFormat[Message] = Message.implMessageFormat
  implicit val messageBlockFormat: OFormat[MessageBlock] = Json.format[MessageBlock]
  implicit val chatRoomDocumentFormat: OFormat[ChatRoomDocument] = Json.format[ChatRoomDocument]

  val chatRoomCollection: MongoCollection[Document] = chatdb.getCollection("ChatRooms")
  val messagesCollection: MongoCollection[Document] = chatdb.getCollection("messages")
  messagesCollection.createIndex(Indexes.ascending("roomId", "timestamp"))

  private val chatActors = TrieMap[Int, ActorRef]()
  private val userActors = TrieMap[Int, ActorRef]()

  override def postStop(): Unit = {
    saveScheduler.cancel()
    super.postStop()
  }

  def receive: Receive = {

    case GetRoom(roomId) => chatActors.get(roomId) match {

      case Some(value) => sender() ! RoomRef(value, roomId)
      case None =>
        val filter = Filters.eq("_id", roomId)

        (for {
          chatRoomDocument <- chatRoomCollection.find(filter).first().toFutureOption()
          if chatRoomDocument.isDefined

          chatRoom = Json.parse(chatRoomDocument.get.toJson).as[ChatRoomDocument]
        } yield {
          val roomActor = context.actorOf(Props(new ChatRoomActor(ChatRoom(roomId, chatRoom.members, Seq.empty), self)(ec)))
          chatActors += roomId -> roomActor
          RoomRef(roomActor, roomId)
        }).fallbackTo(
          Future.successful(RoomNotAvailable("Room not found!"))
        ).pipeTo(sender())
    }

    case SaveRooms() =>
      println(s"[ChatService] - Saving All Rooms")
      chatActors.values.foreach(_ ! SaveSnapshot())

    case WriteSnapshot(chatRoom) =>
      println(s"[ChatService] - Received ChatRoom snapshot, Saving.")
      val blocks = chatRoom.messages.filter(_.messageBlock.isEmpty).sorted.grouped(30).map { messages =>
        val id = UUID.randomUUID()
        messages.foreach(_.messageBlock = Some(id))
        val block = MessageBlock(id, messages.toArray, chatRoom.id, messages.head.dateTime)

        messagesCollection.insertOne(Document(Json.toJson(block).toString())).toFuture().recover {
          case ex => println(s"[ChatService] Error inserting message block: $ex")
        }
        id
      }.toSeq

      val filter = Filters.eq("_id", chatRoom.id)

      chatRoomCollection.find(filter).first().headOption().flatMap {
        case Some(_) =>
          val updates = Updates.combine(
            Updates.addEachToSet("blocks", blocks: _*),
            Updates.set("members", chatRoom.members)
          )
          chatRoomCollection.updateOne(filter, updates).toFuture().recover {
            case ex => println(s"[ChatService] Error updating chat room: $ex")
          }

        case None =>
          val chatRoomDoc = ChatRoomDocument(chatRoom.id, chatRoom.members, blocks)
          chatRoomCollection.insertOne(Document(Json.toJson(chatRoomDoc).toString)).toFuture().recover {
            case ex => println(s"[ChatService] Error inserting chat room: $ex")
          }
      }.recover {
        case ex => println(s"[ChatService] Error finding chat room: $ex")
      }

    case RemoveRoom(roomId) =>
      chatActors.get(roomId).foreach { actor =>
        chatActors.remove(roomId)
        context.stop(actor)
      }

    case CoupleUser(user) =>
      val (queue, source) = Source.queue[JsValue](16, OverflowStrategy.backpressure).preMaterialize()

      val userActor = context.actorOf(Props(new UserActor(user, queue, self)(ec)))

      val sink = Flow[JsValue]
        .throttle(5, 1.second, 5, ThrottleMode.Shaping)
        .to(Sink.foreach(msg => {
          msg.validate[UserAction] match {
            case play.api.libs.json.JsSuccess (userAction, _) =>
              userActor ! userAction
            case play.api.libs.json.JsError(errors) =>
              println(s"[ChatService] Invalid UserAction JSON: $msg, errors: $errors")
          }
        }))
      println("[ChatService] Making Flow")

      val flow = Flow.fromSinkAndSourceCoupledMat(sink, source)(Keep.both).watchTermination() {
        case ((_, _), termination) =>
          termination.onComplete { _ =>
            println(s"[ChatService] WebSocket closed, stopping actor for ${user.value}")
            userActors -= user.value
            context.stop(userActor)
          }
      }
      sender() ! flow

      userActors += user.value -> userActor
      chatActors.values.foreach(_ ! OpenRoomToMember(user, userActor))


    case GetMessageBlock(roomId, timestamp) =>
      val roomFilter = Filters.eq("roomId", roomId)
      val combinedFilters = timestamp match {
        case Some(pt) =>
          Filters.and(roomFilter, Filters.lt("timestamp", pt))
        case None =>
          roomFilter
      }
      messagesCollection.find(combinedFilters).sort(Sorts.descending()).first().toFutureOption().map(m =>
        Json.parse(m.get.toJson).as[MessageBlock]
      ).fallbackTo(
        Future.successful(MessagesNotAvailable("reason"))
      ).pipeTo(sender())


    case CreateRoom(creator) =>
      val roomId = UUID.randomUUID().hashCode()
      val room = ChatRoom(roomId, Seq(creator), Seq.empty)
      val actor = context.actorOf(Props(new ChatRoomActor(room, self)(ec)))
      chatActors += roomId -> actor
      sender() ! RoomRef(actor, roomId)

    case AnnounceRoom(users) =>
      users.foreach(user => userActors.get(user.value).foreach(_ ! OpenedRoom(sender())))


    case unknown =>
      println(s"[ChatService] Received unknown message: $unknown")
  }
}