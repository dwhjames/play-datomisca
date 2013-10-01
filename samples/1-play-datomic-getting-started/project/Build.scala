import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName               = "play-datomisca-getting-started"
    val appVersion            = "1.0-SNAPSHOT"
    val datomicVersion        = "0.8.4020.26"
    val playDatomiscaVersion  = "0.5.2"

    val appDependencies = Seq(
      "com.pellucid" %% "play-datomisca" % playDatomiscaVersion,
      "com.datomic" % "datomic-free" % datomicVersion exclude("org.slf4j", "slf4j-nop")      
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq(
        Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns),
        "clojars" at "https://clojars.org/repo",
        "Couchbase" at "http://files.couchbase.com/maven2/",
        "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",    
        "datomisca-repo releases" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases"   
      ),
      scalacOptions       ++= Seq("-deprecation", "-feature", "-unchecked")
    )

}
