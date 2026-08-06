package chat

import model.{Identity, User}
import org.apache.pekko.actor.ActorRef

import java.time.Instant


case class SaveRooms()
case class RemoveRoom(roomId: Int)
case class WriteSnapshot(snapshot: ChatRoom)

case class GetRoom(roomId: Int)
case class RoomNotAvailable(reason: String)
case class RoomRef(ref: ActorRef, roomId: Int)

case class GetMessageBlock(roomId: Int, lastTakenMessageIndex: Option[Instant])
case class MessagesNotAvailable(reason: String)

case class CreateRoom(creator: Identity[User])
case class CoupleUser(user: Identity[User])

case class AnnounceRoom(users: Set[Identity[User]])


  
case class Info()
case class OpenRoomToMember(user: Identity[User], userActor: ActorRef)
case class Subscribe(user: Identity[User])
case class Unsubscribe()
case class JoinRoom(user: Identity[User])
case class LeaveRoom(user: Identity[User])
case class SaveSnapshot()

case class ActionFailed(reason: String)
case class ActionSuccess()


case class OpenedRoom(room: ActorRef)
case class ClosedRoom()
