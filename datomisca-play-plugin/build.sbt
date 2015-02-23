
name := "datomisca-play-plugin"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

libraryDependencies ++= {
  import Dependencies.Compile._
  import Dependencies.Test._
  Seq(
    datomic,
    datomisca,
    play23 % "provided",
    playTest23,
    specs2
  )
}

fork in Test := true

Publish.publishSettings
