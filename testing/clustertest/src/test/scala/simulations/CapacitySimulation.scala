package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import simulations.ClusterProtocol._

import scala.concurrent.duration._

class CapacitySimulation extends Simulation {

  private val step = sys.props.get("cluster.step").map(_.toInt).getOrElse(25)
  private val levels = sys.props.get("cluster.levels").map(_.toInt).getOrElse(6)
  private val plateau = 40.seconds

  private val journey = exec(
    http("GET /radio/foryou")
      .get("/radio/foryou?count=10").headers(authHeaders).check(status.is(200))
  ).exec(
    http("GET /posts")
      .get("/posts?limit=20").headers(authHeaders).check(status.is(200))
  ).exec(
    http("GET /search")
      .get("/search?query=the&limit=20").headers(authHeaders).check(status.is(200))
  )

  private val scn = scenario(s"Capacity sweep ($step/s steps)")
    .feed(userFeeder.circular)
    .exec(login)
    .exitHereIfFailed
    .exec(journey)

  setUp(
    scn.inject(
      incrementUsersPerSec(step)
        .times(levels)
        .eachLevelLasting(plateau)
        .startingFrom(step)
    ).protocols(httpProtocol)
  )
}
