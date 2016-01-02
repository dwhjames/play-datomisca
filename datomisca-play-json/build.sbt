
name := "datomisca-play-json"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

libraryDependencies ++= {
  import Dependencies.Compile._
  Seq(
    datomic,
    datomisca,
    play24
  )
}

Publish.publishSettings
