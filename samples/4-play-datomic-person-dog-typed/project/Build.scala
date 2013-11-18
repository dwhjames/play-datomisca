import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName               = "play-datomisca-person-dog-typed"
    val appVersion            = "1.0-SNAPSHOT"
    val datomicVersion        = "0.8.4260"
    val playDatomiscaVersion  = "0.6"

    val appDependencies = Seq(
      "com.pellucid" %% "play-datomisca" % playDatomiscaVersion,
      "com.datomic" % "datomic-free" % datomicVersion exclude("org.slf4j", "slf4j-nop")      
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      //scalacOptions ++= Seq("-Xlog-implicits"),
      resolvers ++= Seq(
        "Pellucid Bintray" at "http://dl.bintray.com/content/pellucid/maven",
        "clojars" at "https://clojars.org/repo"
      )
    )

}
