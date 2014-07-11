
name := "datomisca-play22-json"

scalaVersion := "2.10.4"

libraryDependencies ++= {
  import Dependencies.Compile._
  Seq(
    datomic,
    datomisca,
    play22
  )
}

Publish.publishSettings
