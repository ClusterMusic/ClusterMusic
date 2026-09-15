name         := "cluster-dummy"
organization := "com.clustermusic"
version      := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

val playSlickVersion = "6.1.0"
val mongoDriverVersion = "4.11.1"
val jwtVersion = "10.0.1"

libraryDependencies ++= Seq(
  guice,                      
  filters,    // cors            

  
  "org.playframework" %% "play-slick" % playSlickVersion,
  "com.mysql"          % "mysql-connector-j" % "8.4.0",

  
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoDriverVersion,

  "com.github.jwt-scala" %% "jwt-core" % jwtVersion
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play"        % "7.0.1"  % Test,
  "org.scalatestplus"      %% "scalacheck-1-17"           % "3.2.18.0" % Test,
  "org.apache.pekko"       %% "pekko-testkit"             % "1.0.3"  % Test,
  "org.apache.pekko"       %% "pekko-actor-testkit-typed" % "1.0.3"  % Test
)

PlayKeys.playDefaultPort := 9000

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Wconf:src=routes/.*:silent",
  "-Wconf:src=twirl/.*:silent"
)
