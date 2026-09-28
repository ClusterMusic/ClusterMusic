package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import simulations.ClusterProtocol._

import scala.concurrent.duration._


class FeedSimulation extends Simulation {

  private val browseFeed = exec(
    http("GET /radio/foryou")
      .get("/radio/foryou?count=10")
      .headers(authHeaders)
      .check(status.is(200))
  ).pause(1, 3)
    .exec(
      http("GET /posts")
        .get("/posts")
        .headers(authHeaders)
        .check(status.is(200))
        .check(jsonPath("$[0].id").optional.saveAs("postId"))
    )
    .pause(1, 3)
    .exec(
      http("GET /clusters")
        .get("/clusters")
        .headers(authHeaders)
        .check(status.is(200))
    )
  private val engageWithPost = doIf(session => session.contains("postId")) {
    exec(
      http("POST /actions/view/post/:id")
        .post("/actions/view/post/#{postId}")
        .headers(authHeaders)
        .check(status.in(200, 201))
    ).pause(1)
      .exec(
        http("POST /actions/like/post/:id")
          .post("/actions/like/post/#{postId}")
          .headers(authHeaders)
          .check(status.in(200, 201))
      )
  }

  private val search = exec(
    http("GET /search")
      .get("/search?query=the&limit=20")
      .headers(authHeaders)
      .check(status.is(200))
  )

  private val scn = scenario("Feed browse and engage")
    .feed(userFeeder.circular)
    .exec(login)
    .exec(browseFeed)
    .exec(engageWithPost)
    .pause(1, 2)
    .exec(search)

  setUp(
    scn.inject(
      rampUsersPerSec(1).to(20).during(30.seconds),
      rampUsersPerSec(20).to(100).during(1.minute),
      rampUsersPerSec(100).to(300).during(2.minutes),
      constantUsersPerSec(300).during(1.minute)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile3.lt(2000),
    global.successfulRequests.percent.gt(95),
    details("POST /auth/login").responseTime.percentile3.lt(1000)
  )
}
