package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

import reactivedatomic.Datomic._ 
import reactivedatomic._
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
      Datomic.addEntity(DId(Partition.USER))(
        person / "name" -> "toto",
        person / "age" -> 30
      ),
      Datomic.addEntity(DId(Partition.USER))(
        person / "name" -> "tutu",
        person / "age" -> 54
      ),
      Datomic.addEntity(DId(Partition.USER))(
        person / "name" -> "tata",
        person / "age" -> 23
      )
    ).map{ tx => 
      //println("Inserted data... tx:"+tx)
      Datomic.query[Args2, Args3]("""
      [ 
        :find ?e ?name ?a
        :in $ ?age
        :where [ ?e :person/name ?name ] 
               [ ?e :person/age ?a ]
               [ (< ?a ?age) ]
      ]
      """).all().execute(database, DLong(45L))
        .recover{ 
          case e => Future.failed(e) 
        }
    }
    Async {
      fut.map { tryres =>
        tryres match {
          case Success(t) => Ok(t.toString)
          case Failure(e) => BadRequest(e.getMessage)     
        }
      }
    }
    

  }


}