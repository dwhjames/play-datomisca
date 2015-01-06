name := "play-datomisca-getting-started"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:reflectiveCalls", "-language:implicitConversions")

resolvers ++= Seq(
  "Pellucid Bintray" at "http://dl.bintray.com/content/pellucid/maven",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.datomic" % "datomic-free" % "0.9.5078",
  "com.pellucid" %% "datomisca-play-plugin" % "0.7-alpha-4",
  "com.pellucid" %% "datomisca" % "0.7-alpha-11"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
