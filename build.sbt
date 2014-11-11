name := """StackOverflowBadge"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

val akkaVersion = "2.3.4"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-contrib"                  % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "org.iq80.leveldb"  %  "leveldb"                       % "0.7", // see: https://github.com/akka/akka/issues/15129
  "org.reactivemongo" %% "play2-reactivemongo"           % "0.10.5.0.akka23",
  "com.typesafe.akka" %% "akka-testkit"                  % akkaVersion        % "test",
  "org.scalatest"     %% "scalatest"                     % "2.2.2"            % "test"
)

scoverage.ScoverageSbtPlugin.instrumentSettings

org.scoverage.coveralls.CoverallsPlugin.coverallsSettings

ScoverageKeys.minimumCoverage := 10 // TODO tweak this

ScoverageKeys.failOnMinimumCoverage := true

ScoverageKeys.excludedPackages in ScoverageCompile := List(
  "controllers\\..*"
).mkString(";")
