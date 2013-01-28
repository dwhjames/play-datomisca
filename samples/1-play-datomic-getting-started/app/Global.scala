import play.api._

import play.modules.datomic._
import reactivedatomic._

import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.io.Source

object Global extends GlobalSettings {
  import play.api.Play.current

  override def onStart(app: Application){
    val uri = DatomicPlugin.uri("seattle")
        
    play.Logger.info("created DB:" + Datomic.createDatabase(uri))

    val schemaIs = current.resourceAsStream("seattle-schema.dtm").get
    //val schemaContent = Source.fromInputStream(schemaIs).getLines.reduceLeft(_ + _)
    val schemaContent = Source.fromInputStream(schemaIs).mkString
    val schema = Datomic.parseOps(schemaContent)
    println("schema:"+schema)

    schema map { schema =>
      implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext
      implicit val conn = Datomic.connect(uri)
      val fut = Datomic.transact(schema) flatMap { tx =>
        play.Logger.info("bootstrapped schema")

        val dataIs = current.resourceAsStream("seattle-data0.dtm").get
              
        val dataContent = Source.fromInputStream(dataIs).mkString
        val data = Datomic.parseOps(dataContent)

        data map { data =>
          Datomic.transact(data) map { tx =>
            play.Logger.info("bootstrapped data with %d entities".format(tx.tempids.size))
          }
        } get
      }
      
      Await.result(fut, Duration("3 seconds"))
    } get

  }

  override def onStop(app: Application){
    val uri = DatomicPlugin.uri("seattle")

    play.Logger.info("deleted DB:" + Datomic.deleteDatabase(uri))
  }
}