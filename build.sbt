name := """StackOverflowBadge"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" % "akka-contrib_2.11" % "2.3.4",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.0-SNAPSHOT"
)
