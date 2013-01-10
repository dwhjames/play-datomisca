import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName         = "play-datomic-sample"
    val appVersion      = "1.0-SNAPSHOT"
    val datomicVersion  = "0.8.3731"

    val appDependencies = Seq(
      "play.modules.datomic" %% "datomic" % "0.1-SNAPSHOT",
      "com.datomic" % "datomic-free" % datomicVersion exclude("org.slf4j", "slf4j-nop")      
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq(
        Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns),
        "clojars" at "https://clojars.org/repo",
        "couchbase" at "http://files.couchbase.com/maven2"
        //"Bitbucket.org HTTP" at "https://bitbucket.org/mandubian/datomic-mvn/raw/master/releases/"
      )
    )

}
