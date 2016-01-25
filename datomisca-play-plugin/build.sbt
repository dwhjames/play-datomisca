
name := "datomisca-play-plugin"

organization := "com.github.dwhjames"

version := "0.7.0"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  Resolver.bintrayRepo("dwhjames", "maven")
)

/*
libraryDependencies ++= {
  import Dependencies.Compile._
  import Dependencies.Test._
  Seq(
    datomic,
    datomisca,
    play24 % "provided",
    specs2 % Test
  )
}
*/


// Used this to cd into this dir and compile directly.
// For some reason when using activator from the parent project it was creating empty jar.
libraryDependencies ++= {
  Seq(
    "com.datomic" % "datomic-free" % "0.9.5344" % "provided" exclude("org.slf4j", "slf4j-nop") exclude("org.jboss.netty", "netty"),
    "com.github.dwhjames" %% "datomisca" % "0.7.0",
    "com.typesafe.play" %% "play" % "2.4.6" % "provided",
    "com.typesafe.play" %% "play-test" % "2.4.6" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.4.6" % "test",
    "org.specs2" %% "specs2-core" % "3.3.1" % "test"
  )
}


fork in Test := true

