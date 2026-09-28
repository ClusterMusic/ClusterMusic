val scala2Version = "2.13.14"
val gatlingVersion = "3.11.5"

lazy val root = project
  .in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    name := "ClusterTest",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala2Version,

    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test,
      "io.gatling"            % "gatling-test-framework"    % gatlingVersion % Test
    )
  )
