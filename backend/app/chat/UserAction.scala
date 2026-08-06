package chat

import model.{Identity, Post, User}
import play.api.libs.json.{JsError, JsObject, JsResult, JsString, JsValue, Json, Reads, Writes, JsSuccess}

import java.time.Instant

sealed trait UserAction {

}

object UserAction {
  private var userActionsReads: Map[String, Reads[UserAction]] = Map()


  private def mapReads[T <: UserAction](typ: String)(implicit r: Reads[T]): Unit = {
    userActionsReads = userActionsReads + (typ -> r.asInstanceOf[Reads[UserAction]])
  }

  case class SendChat(sendChatMessage: String) extends UserAction
  implicit val sendChatReads: Reads[SendChat] = Json.reads[SendChat]
  mapReads[SendChat]("sendChat")

  case class SendPost(sendPostMessage: Identity[Post]) extends UserAction
  implicit val sendPostReads: Reads[SendPost] = Json.reads[SendPost]
  mapReads[SendPost]("sendPost")

  case class GetSnapshot() extends UserAction
  implicit val getSnapshotReads: Reads[GetSnapshot] = Reads(_ => JsSuccess(GetSnapshot()))
  mapReads[GetSnapshot]("getSnapshot")

  case class GetMessages(timestamp: Instant, limit: Int) extends UserAction
  implicit val getMessagesReads: Reads[GetMessages] = Json.reads[GetMessages]
  mapReads[GetMessages]("getMessages")

  case class JoinRoom(roomId: Int) extends UserAction
  implicit val joinRoomReads: Reads[JoinRoom] = Json.reads[JoinRoom]
  mapReads[JoinRoom]("joinRoom")

  case class LeaveRoom() extends UserAction
  implicit val leaveRoomReads: Reads[LeaveRoom] = Reads(_ => JsSuccess(LeaveRoom()))
  mapReads[LeaveRoom]("leaveRoom")

  case class Exit() extends UserAction
  implicit val exitReads: Reads[Exit] = Reads(_ => JsSuccess(Exit()))
  mapReads[Exit]("exit")

  case class GetRooms() extends UserAction
  implicit val getRoomsReads: Reads[GetRooms] = Reads(_ => JsSuccess(GetRooms()))
  mapReads[GetRooms]("getRooms")


  implicit val implUserActionReads: Reads[UserAction] = (json: JsValue) => {
    val typOpt = (json \ "typ").asOpt[String]
    typOpt match {
      case Some(typ) => if (userActionsReads.contains(typ)) {
        userActionsReads(typ).reads(json.as[JsObject] - "typ")
      } else {
        JsError("'typ' is not a possible action")
      }
      case None => JsError("cannot receive UserAction without 'typ'")
    }

  }

}

