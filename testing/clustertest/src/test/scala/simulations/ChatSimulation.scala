package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import simulations.ClusterProtocol._

import scala.concurrent.duration._


class ChatSimulation extends Simulation {

  private val holdSocket = exec(
    ws("WS connect /chat/ws")
      .connect("/chat/ws?user=#{userId}&access_token=#{accessToken}")
  )
    .pause(2)
    .exec(
      ws("WS getRooms").sendText("""{"typ":"getRooms"}""")
    )
    .pause(20, 40)
    .exec(ws("WS close").close)

  private val scn = scenario("Chat socket capacity")
    .feed(userFeeder.circular)
    .exec(login)
    .exec(holdSocket)

  setUp(
    scn.inject(
      rampUsers(50).during(30.seconds),
      rampUsers(200).during(1.minute),
      rampUsers(500).during(2.minutes)
    ).protocols(httpProtocol)
  ).assertions(
    global.failedRequests.percent.lt(5.0)
  )
}
