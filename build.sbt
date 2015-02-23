
organization in ThisBuild := "com.github.dwhjames"

licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

version in ThisBuild := "0.7-RC1"



scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")


resolvers in ThisBuild ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.bintrayRepo("dwhjames", "maven"),
  "clojars" at "https://clojars.org/repo"
)



lazy val datomiscaPlayJson = project in file("datomisca-play-json")

lazy val datomiscaPlay22Json = (
  project
  in file("datomisca-play22-json")
  settings (
    sourceDirectory <<= sourceDirectory in datomiscaPlayJson
  )
)

lazy val datomiscaPlayPlugin = project in file("datomisca-play-plugin")

lazy val datomiscaPlay22Plugin = (
  project
  in file("datomisca-play22-plugin")
  settings (
    sourceDirectory <<= sourceDirectory in datomiscaPlayPlugin
  )
)
