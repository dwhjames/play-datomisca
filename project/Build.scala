import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "play.modules.datomisca"
  val buildName = "play-datomisca"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.10.0"

  val datomicVersion    = "0.8.3731"

  val playVersion  = "2.1-RC2"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object ApplicationBuild extends Build {
  val typesafeRepo = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  )

  //lazy val datomicDriver = RootProject(uri("https://github.com/pellucidanalytics/datomic.git#master"))

  lazy val playDatomic = Project(
    BuildSettings.buildName, file("."),
    settings = BuildSettings.buildSettings ++ Defaults.defaultSettings ++ Seq(
      shellPrompt := ShellPrompt.buildShellPrompt,
      resolvers ++= typesafeRepo,
      libraryDependencies ++= Seq(
        "play" %% "play" % BuildSettings.playVersion,
        "pellucidanalytics" %% "datomisca" % "0.1-SNAPSHOT",
        "com.datomic" % "datomic-free" % BuildSettings.datomicVersion % "provided" 
          exclude("org.slf4j", "slf4j-nop")
          exclude("org.jboss.netty", "netty"),
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