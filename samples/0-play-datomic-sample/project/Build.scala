import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName           = "play-datomisca-sample"
    val appVersion        = "1.0-SNAPSHOT"
    val datomicVersion    = "0.8.3814"
    val datomiscaVersion  = "0.1"

    val appDependencies = Seq(
      "play.modules.datomisca" %% "play-datomisca" % datomiscaVersion,
      "com.datomic" % "datomic-free" % datomicVersion exclude("org.slf4j", "slf4j-nop")      
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq(
        Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns),
        "clojars" at "https://clojars.org/repo",
        "couchbase" at "http://files.couchbase.com/maven2",
        "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",    
        "datomisca-repo releases" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases"   
      )
    )

}
