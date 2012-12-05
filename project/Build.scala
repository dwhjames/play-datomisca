import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "play.modules.datomic"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion      = "2.10.0-RC1"

  val playVersion  = "2.1-RC1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object ApplicationBuild extends Build {
  val typesafeRepo = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
    Resolver.file("local repository", file("/Volumes/PVO/work/play-2.1-RC1/repository/local"))(Resolver.ivyStylePatterns)
  )

  //lazy val datomicDriver = RootProject(uri("https://github.com/pellucidanalytics/datomic.git#master"))

  lazy val playDatomic = Project(
    "datomic", file("."),
    settings = BuildSettings.buildSettings ++ Defaults.defaultSettings ++ Seq(
      shellPrompt := ShellPrompt.buildShellPrompt,
      resolvers ++= typesafeRepo,
      libraryDependencies ++= Seq(
        "play" %% "play" % BuildSettings.playVersion,
        "pellucid" %% "datomic" % "0.1-SNAPSHOT",
        /*"org.clojure" % "clojure" % "1.4.0",
        "org.clojure" % "data.json" % "0.1.2",
        "net.java.dev.jets3t" % "jets3t" % "0.8.1",
        "org.hornetq" % "hornetq-core" % "2.2.2.Final",
        "com.h2database" % "h2" % "1.3.165",
        "org.infinispan" % "infinispan-client-hotrod" % "5.1.2.FINAL",
        "org.apache.lucene" % "lucene-core" % "3.3.0",
        "com.google.guava" % "guava" % "12.0.1", //dans play
        "spy" % "spymemcached" % "2.8.1", 
        "org.apache.tomcat" % "tomcat-jdbc" % "7.0.27", 
        "postgresql" % "postgresql" % "9.1-901.jdbc4", 
        "org.codehaus.janino" % "commons-compiler-jdk" % "2.6.1",
        "commons-net" % "commons-net" % "3.1",*/

        "play" %% "play-test" % BuildSettings.playVersion % "test",
        "org.specs2" % "specs2_2.10.0-RC1" % "1.12.2" % "test",
        "junit" % "junit" % "4.8" % "test"
      )
    )
  )/*.dependsOn(datomicDriver).aggregate(datomicDriver)*/

  object ShellPrompt {
    object devnull extends ProcessLogger {
      def info (s: => String) {}
      def error (s: => String) { }
      def buffer[T] (f: => T): T = f
    }

    val current = """\*\s+([\w-]+)""".r

    def gitBranches = ("git branch --no-color" lines_! devnull mkString)

    val buildShellPrompt = {
      (state: State) => {
        val currBranch =
          current findFirstMatchIn gitBranches map (_ group(1)) getOrElse "-"
        val currProject = Project.extract (state).currentProject.id
        "%s:%s> ".format (
          currProject, currBranch
        )
      }
    }
  }
}