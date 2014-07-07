
organization in ThisBuild := "com.pellucid"

licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

version in ThisBuild := "0.7-alpha-4"



scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")


resolvers in ThisBuild ++= Seq(
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Pellucid Bintray" at "http://dl.bintray.com/content/pellucid/maven"
)


val pellucidBintrayOrg = bintray.Keys.bintrayOrganization in (bintray.Keys.bintray) := Some("pellucid")



lazy val playDatomisca = project.
  in(file(".")).
  settings(bintray.Plugin.bintraySettings:_*).
  settings(
    name := "play-datomisca",
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    pellucidBintrayOrg,
    libraryDependencies ++= Dependencies.playDatomisca,
    fork in Test := true
  )

lazy val playDatomisca22 = project.
  in(file("oldplay")).
  settings(bintray.Plugin.bintraySettings:_*).
  settings(
    name := "play-datomisca-22",
    scalaVersion := "2.10.4",
    pellucidBintrayOrg,
    sourceDirectory in Compile <<= sourceDirectory in (playDatomisca, Compile),
    sourceDirectory in Test <<= sourceDirectory in (playDatomisca, Test),
    libraryDependencies ++= Dependencies.playDatomisca22,
    fork in Test := true
  )
