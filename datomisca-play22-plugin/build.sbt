
name := "datomisca-play22-plugin"

scalaVersion := "2.10.4"

libraryDependencies ++= {
  import Dependencies.Compile._
  import Dependencies.Test._
  Seq(
    datomic,
    datomisca,
    play22 % "provided",
    playTest22,
    specs2
  )
}

fork in Test := true

Publish.publishSettings
