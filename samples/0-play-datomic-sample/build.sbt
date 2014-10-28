name := "play-datomisca-sample"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers ++= Seq(
  "Pellucid Bintray" at "http://dl.bintray.com/content/pellucid/maven",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.datomic" % "datomic-free" % "0.9.4766.16",
  "com.pellucid" %% "datomisca-play-plugin" % "0.7-alpha-4"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)