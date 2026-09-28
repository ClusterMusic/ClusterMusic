<div align="center">
  <img width="473" alt="image" src="https://github.com/user-attachments/assets/ea5148de-f4e8-4133-9355-5c13116fd10d" />
</div>


Cluster is designed to be a music-centric social networking app that largely groups users by geographic communities such as universities. This geographic focus seeks to embed the app into the daily social lives of users, rather than replace it. By restricting music discovery and sharing to real-world communities, Cluster connects users around local music interests, providing more engaging and relevant content to users and positioning the app within social circles, increasing engagement and fostering both online activity and real-world experiences/events related to Cluster.
 
Notably, Cluster will have an optional yet competitive element to drive engagement from the real world and set us apart from traditional social media. Moreover, this competitive element will allow users to organize around smaller social circles to compete against the community as a whole.

Cluster will be **entirely open-source** for any individual to explore with custom-built chat systems, cybersecurity, recommendation algorithms, and more.

We welcome you to the music game!


[VIDEO GOES HERE]


## Testing

Modify .env and test.scala to point to correct URLs

```bash
cd backend && docker compose up -d   
sbt test                             # unit + integration suite
sbt stage && target/universal/stage/bin/cluster-dummy   # backend on :9000

cd ../testing/clustertest
sbt "Gatling/testOnly simulations.FeedSimulation"      
sbt "Gatling/testOnly simulations.CapacitySimulation"  
```

Gatling writes an HTML report to `target/gatling/<simulation>-<timestamp>/index.html`.

### Unit and integration tests

**65 tests, 0 failures**, ~3.7s.

| Suite | Tests | What it covers |
|---|---:|---|
| [`model.ScoringSpec`](backend/test/model/ScoringSpec.scala) | 24 | Consistent scoring properties over the ranking algorithm: scores stay finite and non-negative, move monotonically with engagement, decay with age, and comments outweigh likes |
| [`chat.ChatRoomActorSpec`](backend/test/chat/ChatRoomActorSpec.scala) | 17 | Coverage of the chat room actor: subscribe/join/leave transitions, broadcast fan-out, room isolation, snapshot persistence, lifecycle cleanup |
| [`auth.GuardianSpec`](backend/test/auth/GuardianSpec.scala) | 13 | JWT issuing and validation: forgery, payload tampering, expiry, and downgrade attack |
| [`auth.AuthorizationSpec`](backend/test/auth/AuthorizationSpec.scala) | 11 | Authorized action proper validation and rejections |

### Load test results (2024 M4 Macbook)

Measured against a realistic dataset:
| Table | Rows |
|---|---:|
| `post` | 200,010 |
| `comment` | 200,010 |
| `user` | 50,005 |
| `song` | 20,010 |
| `user_post_association` | 63,125 |

Per-endpoint latency:
| Endpoint | p50 |
|---|---:|
| `GET /clusters` | 4 ms |
| `GET /posts?limit=20` | 7 ms |
| `GET /radio/community/:id` | 12 ms |
| `GET /radio/cluster/:id` | 15 ms |
| `GET /radio/foryou` | 16 ms |
| `GET /search` | 20 ms |

Request laod throughput:
| Offered load | Throughput | Success | p50 | p95 |
|---:|---:|---:|---:|---:|
| 100 req/s | 100 req/s | 100% | 13 ms | 48 ms |
| **200 req/s** | **200 req/s** | **100%** | **11 ms** | **73 ms** |
| 300 req/s | 149 req/s | 80.2% | 1,972 ms | 18,164 ms |
| 400 req/s | 149 req/s | 64.1% | 2,282 ms | 18,956 ms |
| 500 req/s | 117 req/s | 50.8% | 1,872 ms | 21,744 ms |
| 600 req/s | 123 req/s | 46.3% | 2,003 ms | 17,219 ms |


The load generator shares a machine with the server, so a large share of failures past are client-side `ConnectTimeoutException` rather than server errors. The
sustained figure above is therefore a floor on server capacity, not a ceiling.

### Test environment

All figures above were measured on:

| | |
|---|---|
| Machine | Apple M4 Pro, 14 cores (10 performance / 4 efficiency), 48 GB RAM, arm64 |
| OS | macOS 26.2 (25C56) |
| JVM | OpenJDK 17.0.20 |
| Backend | Play 3.0.3, Scala 2.13.14 |
| Database | MySQL 8.0.46 in Docker, 128 MB InnoDB buffer pool |
| Chat store | MongoDB 6.0 in Docker |
| Connection pool | Slick, 64 threads / 64 connections / 5,000 queue |
| Load generator | Gatling 3.11.5, same machine as the server |

## Documentation

More information about the ClusterMusic application can be found at the following link: https://docs.google.com/document/d/1gbI6useGhHQsjXu1rze9xwgLU5srtOQOM3Jz71vX5Yk/edit?usp=sharing

NOTE: Documentation is incomplete. 
