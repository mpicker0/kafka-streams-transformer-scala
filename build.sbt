organization  := "mike.test"

version       := "0.1"

scalaVersion  := "2.12.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "2.3.0",
  "org.apache.kafka" % "kafka-streams" % "2.3.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
