name := "play-datomisca-person-dog-em"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.bintrayRepo("dwhjames", "maven"),
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  specs2 % Test,
  "com.datomic" % "datomic-free" % "0.9.5344",
  "com.github.dwhjames" %% "datomisca-play-plugin" % "0.7.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)