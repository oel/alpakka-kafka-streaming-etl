name := "alpakka-kafka-streaming-etl"

version := "0.1"

scalaVersion := "2.13.6"

scalacOptions += "-deprecation"

val akkaVersion = "2.6.16"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "3.0.3",
  "org.postgresql" % "postgresql" % "42.2.24",
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.3",
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "3.0.3",
  "com.datastax.oss" % "java-driver-core" % "4.13.0",
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.5.1",
  "io.spray" %%  "spray-json" % "1.3.6",
  "ch.qos.logback" % "logback-classic" % "1.2.4" % Runtime
)

trapExit := false
