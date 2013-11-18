
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import play.api.{Application, GlobalSettings}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import datomisca._
import play.modules.datomisca._

import models._

object Global extends GlobalSettings {
  import play.api.Play.current

  override def onStart(app: Application){
    val uri = DatomicPlugin.uri("mem")
        
    play.Logger.info("created DB:" + Datomic.createDatabase(uri))

    implicit val conn = Datomic.connect(uri)

    // TODO : here we should verify first that schema is not already in DB
    val fut = Datomic.transact(
      Seq(
        Fact.partition(Common.MY_PART)
      ) ++
      Dog.schema ++ Person.schema
    ) map { tx =>
      println("bootstrapped schema")
    }
    
    // blocking to be sure schema is in DB
    Await.result(fut, Duration("3 seconds"))

  }

  override def onStop(app: Application){
    val uri = DatomicPlugin.uri("mem")

    play.Logger.info("deleted DB:" + Datomic.deleteDatabase(uri))
  }
}
