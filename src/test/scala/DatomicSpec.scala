import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import play.api.test._
import play.api.test.Helpers._

import play.modules.datomisca._
import datomisca._

import scala.concurrent._
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DatomicSpec extends Specification {
  "DatomicPlugin" should {
    "read conf" in {
      import scala.collection.JavaConverters._

      Utils.running(FakeApplicationWithConf(
        additionalConfiguration = 
          com.typesafe.config.ConfigFactory.parseMap(Map(
            "datomisca.uri.mem" -> "datomic:mem://mem",
            "datomisca.uri.mem2" -> "datomic:mem://mem2"
          ).asJava),
        additionalPlugins = Seq("play.modules.datomisca.DatomicPlugin")
      )){
        import play.api.Play.current

        val uri = DatomicPlugin.uri("mem")
        
        uri must beEqualTo("datomic:mem://mem")
        DatomicPlugin.safeUri("memdfds") must beEqualTo(None)

        println("creating DB")
        Datomic.createDatabase(uri) must beEqualTo(true)
        
        implicit val conn = DatomicPlugin.connect("mem")

        conn must not beNull

        val person = new Namespace("person") {
          val character = Namespace("person.character")
        }

        val violent = AddIdent(person.character / "violent")
        val weak    = AddIdent(person.character / "weak")
        val clever  = AddIdent(person.character / "clever")
        val dumb    = AddIdent(person.character / "dumb")
        val stupid  = AddIdent(person.character / "stupid")

        val schema = Seq(
          Attribute(person / "name",      SchemaType.string, Cardinality.one) .withDoc("Person's name"),
          Attribute(person / "age",       SchemaType.long,   Cardinality.one) .withDoc("Person's age"),
          Attribute(person / "character", SchemaType.ref,    Cardinality.many).withDoc("Person's characterS"),
          violent,
          weak,
          clever,
          dumb,
          stupid
        )

        //implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext
        import ExecutionContext.Implicits.global

        val fut = Datomic.transact(schema) flatMap { tx =>
          println("Created schema")
          Datomic.transact(
            Entity.add(DId(Partition.USER))(
              person / "name" -> "toto",
              person / "age" -> 30
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age" -> 54
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age" -> 23
            )
          ) map { tx =>
            //println("Inserted data... tx:"+tx)
            val query = Query("""
              [
                :find ?e ?name ?a
                :in $ ?age
                :where [ ?e :person/name ?name ]
                       [ ?e :person/age ?a ]
                       [ (< ?a ?age) ]
              ]
            """)

            val results = Datomic.q(query, Datomic.database, DLong(45L))
            println("results:"+results)
          }
        }

        Await.result(
          fut,
          Duration("10 seconds")
        )

        Datomic.deleteDatabase(uri) must beEqualTo(true)
        println("deleting DB")
        success
      }
    }
  }
}
