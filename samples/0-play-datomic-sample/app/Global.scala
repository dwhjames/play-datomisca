import play.api._

import play.modules.datomic._
import reactivedatomic._

import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration

object Global extends GlobalSettings {
  import play.api.Play.current

  override def onStart(app: Application){
    val uri = DatomicPlugin.uri("mem")
        
    play.Logger.info("created DB:" + Datomic.createDatabase(uri))

    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }

    val violent = AddIdent(person.character / "violent")
    val weak    = AddIdent(person.character / "weak")
    val clever  = AddIdent(person.character / "clever")
    val dumb    = AddIdent(person.character / "dumb")
    val stupid  = AddIdent(person.character / "stupid")

    val schema = Seq(
      Attribute(person / "name",      SchemaType.string, Cardinality.one) .withDoc("Person's name").withUnique(Unique.identity),
      Attribute(person / "age",       SchemaType.long,   Cardinality.one) .withDoc("Person's age"),
      Attribute(person / "character", SchemaType.ref,    Cardinality.many).withDoc("Person's characters"),
      violent,
      weak,
      clever,
      dumb,
      stupid
    )

    implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val conn = Datomic.connect(uri)
    val fut = Datomic.transact(schema) map { tx =>
      println("bootstrapped data")
    }
    
    Await.result(fut, Duration("3 seconds"))

  }

  override def onStop(app: Application){
    val uri = DatomicPlugin.uri("mem")

    play.Logger.info("deleted DB:" + Datomic.deleteDatabase(uri))
  }
}