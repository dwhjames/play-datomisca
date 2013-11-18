package controllers

import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import datomisca._
import play.modules.datomisca._


object Application extends Controller {
  def index = Action.async {
    val uri = DatomicPlugin.uri("mem")
    implicit val conn = Datomic.connect(uri)

    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }

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
      val query = Query("""
      [ 
        :find ?e ?name ?a
        :in $ ?age
        :where [ ?e :person/name ?name ] 
               [ ?e :person/age ?a ]
               [ (< ?a ?age) ]
      ]
      """)

      Datomic.q(query, conn.database, DLong(45L))
    } map { l =>
      Ok(l.toString)
    }
  }
}
