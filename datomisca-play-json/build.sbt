
name := "datomisca-play-json"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4", "2.11.1")

libraryDependencies ++= {
  import Dependencies.Compile._
  Seq(
    datomic,
    datomisca,
    play23
  )
}

Publish.publishSettings
