import sbt._
import Keys._

object Publish {

  val publishSettings: Seq[Setting[_]] =
    bintray.Plugin.bintraySettings :+ (
      bintray.Keys.bintrayOrganization in (bintray.Keys.bintray) := Some("pellucid")
    )
}
