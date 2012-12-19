import sbt._
import Keys._
import play.Project._


object ApplicationBuild extends Build {

    val appName         = "play-datomic-person-dog-typed"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "play.modules.datomic" %% "datomic" % "0.1-SNAPSHOT",
      "com.datomic" % "datomic-free" % "0.8.3627" exclude("org.slf4j", "slf4j-nop")      
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      //scalacOptions ++= Seq("-Xlog-implicits"),
      resolvers ++= Seq(
        Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns),
        "Bitbucket.org HTTP" at "https://bitbucket.org/mandubian/datomic-mvn/raw/master/releases/"
      )
    )

}
