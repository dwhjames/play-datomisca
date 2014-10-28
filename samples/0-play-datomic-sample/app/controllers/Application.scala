package controllers


import datomisca._
import datomisca.plugin.DatomiscaPlayPlugin
import play.api.Play.current
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action.async {

    val uri = DatomiscaPlayPlugin.uri("mem")
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

      Datomic.q(query, conn.database, 45L)
    } map { l =>
      Ok(l.toString)
    }
  }
}
