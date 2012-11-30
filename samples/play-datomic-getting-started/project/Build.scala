import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName         = "play-datomic-getting-started"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "play.modules.datomic" %% "datomic" % "0.1-SNAPSHOT"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq(
        Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns),
        "clojars" at "https://clojars.org/repo"
      )
    )

}
