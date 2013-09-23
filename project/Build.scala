
import scala.language.postfixOps

import sbt._
import Keys._


object PlayDatomiscaBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq (
    organization  :=  "com.pellucid",
    version       :=  "0.5.2",
    scalaVersion  :=  "2.10.2",
    scalacOptions ++= Seq(
        "-deprecation",
        "-feature",
        "-unchecked"
      )
  )

  lazy val playDatomic = Project(
    id       = "play-datomisca",
    base     = file("."),
    settings = playDatomicsaSettings
  )

  val typesafeRepo = Seq(
    "Typesafe repository releases"  at "http://repo.typesafe.com/typesafe/releases/"
  )

  val pellucidRepo = Seq(
    "Pellucid Bintray" at "http://dl.bintray.com/content/pellucid/maven"
  )

  lazy val playDatomicsaSettings =
    buildSettings ++
    bintray.Plugin.bintraySettings ++
    Seq(
      name        := "play-datomisca",
      shellPrompt := CustomShellPrompt.customPrompt,

      resolvers           ++= typesafeRepo ++ pellucidRepo,
      libraryDependencies ++= Dependencies.playDatomicsa,

      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      bintray.Keys.bintrayOrganization in bintray.Keys.bintray := Some("pellucid")
    )

}

object Dependencies {

  object V {
    // compile
    val datomic      = "0.8.4020.26"
    val datomisca    = "0.5.1"
    val play         = "2.2.0"

    // test
    val junit     = "4.8"
    val specs2    = "2.0"
  }

  object Compile {
    val datomic      = "com.datomic"          %  "datomic-free"    % V.datomic    % "provided" exclude("org.slf4j", "slf4j-nop") exclude("org.jboss.netty", "netty")

    val datomisca    = "com.pellucid"         %% "datomisca"       % V.datomisca

    val play         = "com.typesafe.play"    %% "play"            % V.play       % "provided"
  }
  import Compile._

  object Test {
    val playTest    = "com.typesafe.play"    %% "play-test"    % V.play      % "test"
    val specs2      = "org.specs2"           %% "specs2"       % V.specs2    % "test"
    val junit       = "junit"                %  "junit"        % V.junit     % "test"
  }
  import Test._

  val playDatomicsa =
    Seq(
      // compile
      datomic, datomisca, play,
      // test
      playTest, specs2, junit
    )
}

object CustomShellPrompt {

  val Branch = """refs/heads/(.*)\s""".r

  def gitBranchOrSha =
    Process("git symbolic-ref HEAD") #|| Process("git rev-parse --short HEAD") !! match {
      case Branch(name) => name
      case sha          => sha.stripLineEnd
    }

  val customPrompt = { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data) map { name =>
      "[" + scala.Console.CYAN + name + scala.Console.RESET + "] " +
      scala.Console.BLUE + "git:(" +
      scala.Console.RED + gitBranchOrSha +
      scala.Console.BLUE + ")" +
      scala.Console.RESET + " $ "
    } getOrElse ("> ")

  }
}
