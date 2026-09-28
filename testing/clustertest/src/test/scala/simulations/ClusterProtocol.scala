package simulations

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

object ClusterProtocol {

  val baseUrl: String =
    sys.props.get("cluster.baseUrl")
      .orElse(sys.env.get("CLUSTER_BASE_URL"))
      .getOrElse("http://localhost:9000")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("gatling/cluster-loadtest")
    .shareConnections


  val seededUsers: Array[Map[String, String]] = Array(
    Map("username" -> "demo_user", "password" -> "password123"),
    Map("username" -> "indie_fan", "password" -> "password123"),
    Map("username" -> "hip_hop_head", "password" -> "password123"),
    Map("username" -> "rock_lover", "password" -> "password123"),
    Map("username" -> "pop_princess", "password" -> "password123")
  )

  val userFeeder: IndexedSeq[Map[String, String]] = seededUsers.toIndexedSeq

  val login: ChainBuilder = exec(
    http("POST /auth/login")
      .post("/auth/login")
      .body(StringBody("""{"username":"#{username}","password":"#{password}"}""")).asJson
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("accessToken"))
      .check(jsonPath("$.userId").saveAs("userId"))
  )

  val authHeaders: Map[String, String] = Map(
    "Authorization" -> "Bearer #{accessToken}",
    "User" -> "#{userId}"
  )
}
