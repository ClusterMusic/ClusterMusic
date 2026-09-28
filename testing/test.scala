package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class StressTest extends Simulation {

  // ---- 1. HTTP config: point this at your API ----
  val httpProtocol = http
    .baseUrl("https://api.example.com")           // <-- change to your base URL
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("Authorization", "Bearer YOUR_TOKEN_HERE") // empty token for Bad Guardian

    def httpProtocol = 

  // ---- 2. The request(s) being tested ----
  val getOrders = exec(
    http("Get Orders")
      .get("/v1/orders")
      .check(status.is(200))
  )



  val createOrder = exec(
    http("Create Order")
      .post("/v1/orders")
      .body(StringBody("""{"item": "widget", "qty": 1}"""))
      .check(status.in(200, 201))
  )

  // A scenario can mix multiple calls, with think-time between them
  val scn = scenario("Stress Scenario")
    .exec(getOrders)
    .pause(1)
    .exec(createOrder)

  // ---- 3. Injection profile: push load up in stages to find the limit ----
  setUp(
    scn.inject(
      rampUsersPerSec(1).to(20).during(30.seconds),   // warm up
      rampUsersPerSec(20).to(100).during(1.minute),    // ramp harder
      rampUsersPerSec(100).to(300).during(2.minutes),  // push past comfortable
      constantUsersPerSec(300).during(1.minute)        // hold at peak to see if it survives
    ).protocols(httpProtocol)
  ).assertions(
    // These are your "break" signals — Gatling will flag when they fail
    global.responseTime.percentile3.lt(2000),  // p95 under 2s
    global.successfulRequests.percent.gt(95)   // less than 5% error rate
  )
}
