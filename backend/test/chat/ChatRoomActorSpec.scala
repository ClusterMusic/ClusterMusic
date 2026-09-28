package chat

import model.{Identity, User}
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.pekko.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ChatRoomActorSpec
    extends TestKit(ActorSystem("chat-room-spec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  private implicit val ec: ExecutionContext = system.dispatcher
  private implicit val askTimeout: Timeout = 2.seconds

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  private val alice = Identity[User](1)
  private val bob = Identity[User](2)
  private val stranger = Identity[User](99)

  private val roomId = 100

  private def newRoom(
    id: Int = roomId,
    members: Seq[Identity[User]] = Seq(alice, bob),
    messages: Seq[Message] = Seq.empty
  ) = {
    val service = TestProbe()
    val room = system.actorOf(Props(new ChatRoomActor(ChatRoom(id, members, messages), service.ref)))
    service.expectMsg(AnnounceRoom(members.toSet)) // preStart
    (room, service)
  }

  private def subscriber(room: org.apache.pekko.actor.ActorRef, user: Identity[User]): TestProbe = {
    val probe = TestProbe()
    probe.send(room, Subscribe(user))
    probe.expectMsg(ActionSuccess())
    probe
  }

  "A chat room" should {

    "announce itself to the chat service when it starts" in {
      val service = TestProbe()
      system.actorOf(Props(new ChatRoomActor(ChatRoom(7, Seq(alice, bob), Seq.empty), service.ref)))
      service.expectMsg(AnnounceRoom(Set(alice, bob)))
    }

    "let a member subscribe" in {
      val (room, _) = newRoom()
      val probe = TestProbe()
      probe.send(room, Subscribe(alice))
      probe.expectMsg(ActionSuccess())
    }

    "refuse a subscription from someone who is not a member" in {
      val (room, _) = newRoom()
      val probe = TestProbe()
      probe.send(room, Subscribe(stranger))
      probe.expectMsgType[ActionFailed]
    }

    "refuse to open the room for a connection that never subscribed" in {
      val (room, _) = newRoom()
      val probe = TestProbe()
      probe.send(room, JoinRoom(alice))
      probe.expectMsgType[ActionFailed]
    }

    "tell every subscriber when a member joins" in {
      val (room, _) = newRoom()
      val aliceProbe = subscriber(room, alice)
      val bobProbe = subscriber(room, bob)

      aliceProbe.send(room, JoinRoom(alice))

      aliceProbe.expectMsg(ChatEvent.UserJoined(alice, roomId))
      bobProbe.expectMsg(ChatEvent.UserJoined(alice, roomId))
    }
  }

  "A chat message" should {

    "reach every subscriber in the room" in {
      val (room, _) = newRoom()
      val aliceProbe = subscriber(room, alice)
      val bobProbe = subscriber(room, bob)

      aliceProbe.send(room, (UserAction.SendChat("hello"), alice))

      Seq(aliceProbe, bobProbe).foreach { probe =>
        probe.expectMsgPF() {
          case ChatEvent.MessageEvent(ChatMessage(sender, text, _, _), id) =>
            sender shouldBe alice
            text shouldBe "hello"
            id shouldBe roomId
        }
      }
    }

    "not leak into a different room" in {
      val (roomA, _) = newRoom(id = 1)
      val (roomB, _) = newRoom(id = 2)
      val inA = subscriber(roomA, alice)
      val inB = subscriber(roomB, bob)

      inA.send(roomA, (UserAction.SendChat("private to A"), alice))

      inA.fishForSpecificMessage(2.seconds) { case _: ChatEvent.MessageEvent => () }
      inB.expectNoMessage(500.millis)
    }

    "be retained in the room snapshot" in {
      val (room, _) = newRoom()
      val probe = subscriber(room, alice)
      probe.send(room, (UserAction.SendChat("first"), alice))
      probe.receiveN(2)

      probe.send(room, UserAction.GetSnapshot())
      probe.expectMsgPF() {
        case ChatEvent.ResponseSnapshot(ChatRoom(id, members, messages)) =>
          id shouldBe roomId
          members should contain theSameElementsAs Seq(alice, bob)
          messages.collect { case m: ChatMessage => m.chatMessage } shouldBe Seq("first")
      }
    }

    "acknowledge the sender so the client's request completes" in {
      val (room, _) = newRoom()
      subscriber(room, alice)

      val reply = room ? ((UserAction.SendChat("hello"), alice))
      Await.result(reply, 2.seconds) shouldBe a[ChatEvent]
    }
  }

  "Leaving a room" should {

    "tell the remaining subscribers that a viewer left" in {
      val (room, _) = newRoom()
      val aliceProbe = subscriber(room, alice)
      val bobProbe = subscriber(room, bob)
      aliceProbe.send(room, JoinRoom(alice))
      bobProbe.send(room, JoinRoom(bob))
      aliceProbe.receiveN(2)
      bobProbe.receiveN(2)

      aliceProbe.send(room, LeaveRoom(alice))

      bobProbe.expectMsg(ChatEvent.UserLeft(alice, roomId))
    }

    "ask the service to retire the room once the last viewer leaves" in {
      val (room, service) = newRoom()
      val probe = subscriber(room, alice)
      probe.send(room, JoinRoom(alice))
      probe.expectMsgType[ChatEvent.UserJoined]

      probe.send(room, LeaveRoom(alice))

      service.expectMsg(RemoveRoom(roomId))
    }

    "ask the service to retire the room once the last subscriber disconnects" in {
      val (room, service) = newRoom()
      val probe = subscriber(room, alice)

      probe.send(room, Unsubscribe())

      service.expectMsg(RemoveRoom(roomId))
    }

    "keep the room alive while another subscriber remains" in {
      val (room, service) = newRoom()
      val aliceProbe = subscriber(room, alice)
      subscriber(room, bob)

      aliceProbe.send(room, Unsubscribe())

      service.expectNoMessage(500.millis)
    }
  }

  "A stopping room" should {

    "hand its messages to the service to persist" in {
      val (room, service) = newRoom()
      val probe = subscriber(room, alice)
      probe.send(room, (UserAction.SendChat("remember me"), alice))
      probe.expectMsgType[ChatEvent.MessageEvent]

      system.stop(room)

      service.fishForSpecificMessage(3.seconds) {
        case WriteSnapshot(ChatRoom(id, _, messages))
          if id == roomId && messages.collect { case m: ChatMessage => m.chatMessage } == Seq("remember me") =>
          ()
      }
    }

    "notify the connections that still have it open" in {
      val (room, _) = newRoom()
      val probe = subscriber(room, alice)
      probe.send(room, JoinRoom(alice))
      probe.expectMsgType[ChatEvent.UserJoined]

      system.stop(room)

      probe.expectMsg(3.seconds, ClosedRoom())
    }
  }

  "Opening a room for a connection" should {

    "offer the room to a member" in {
      val (room, _) = newRoom()
      val userActor = TestProbe()
      room ! OpenRoomToMember(alice, userActor.ref)
      userActor.expectMsgType[OpenedRoom]
    }

    "stay silent for someone who is not a member" in {
      val (room, _) = newRoom()
      val userActor = TestProbe()
      room ! OpenRoomToMember(stranger, userActor.ref)
      userActor.expectNoMessage(500.millis)
    }
  }
}
