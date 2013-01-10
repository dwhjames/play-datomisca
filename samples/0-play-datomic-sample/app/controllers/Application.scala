package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

import reactivedatomic._
import Datomic._
import play.modules.datomic._

object Application extends Controller {
  def index = Action { 
    val uri = DatomicPlugin.uri("mem")
    implicit val conn = Datomic.connect(uri)

    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }

    implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext
    
    val fut = Datomic.transact(
      Datomic.addToEntity(DId(Partition.USER))(
        person / "name" -> "toto",
        person / "age" -> 30
      ),
      Datomic.addToEntity(DId(Partition.USER))(
        person / "name" -> "tutu",
        person / "age" -> 54
      ),
      Datomic.addToEntity(DId(Partition.USER))(
        person / "name" -> "tata",
        person / "age" -> 23
      )
    ).map{ tx => 
      val query = Datomic.typed.query[Args2, Args3]("""
      [ 
        :find ?e ?name ?a
        :in $ ?age
        :where [ ?e :person/name ?name ] 
               [ ?e :person/age ?a ]
               [ (< ?a ?age) ]
      ]
      """)

      Datomic.q(query, database, DLong(45L))
    }
    Async {
      fut.map { l => Ok(l.toString) }
    }
    

  }


}