package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

import play.api.Play.current
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

import datomisca._
import Datomic._ 
import DatomicMapping._ 

import play.modules.datomisca._
import Implicits._

import models._

object Application extends Controller {
  val uri = DatomicPlugin.uri("mem")
      
  implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val conn = Datomic.connect(uri)

  def index = Action {
    Ok("Ok")
  }

  def insertDog(name: String) = Action {
    Async{
      // generates tmp id
      val dogId = DId(Common.MY_PART)

      // inserts dog
      Datomic.transact(
        SchemaEntity.add(dogId)(Props(
          Dog.name -> name
        ))
      ) map { tx =>
        // resolves real ID
        val realid = tx.resolve(dogId)
        Ok(Json.obj("result" -> "OK", "id" -> realid))        
      }
    }
  }

  def getDog(id: Long) = Action {
    try {
      val entity = database.entity(id)
      Ok(Json.obj(
        "result" -> "OK",
        "data"   -> Json.toJson(entity)(Dog.jsonWrites)
      ))
    } catch {
      case _ =>
        BadRequest(Json.obj("result" -> "KO", "error" -> s"unable to resolve enttiy with id:$id"))
    }
  }

  def insertPerson = Action(parse.json) { request =>
    val json = request.body

    json.validate(Person.jsonReads) map { partialEntity =>
      val personId = DId(Common.MY_PART)
      Async {
        Datomic.transact( Entity.add(personId, partialEntity) ) map { tx =>
          val realId = tx.resolve(personId)
          Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId)))          
        }
      }
    } recoverTotal { e =>
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> JsError.toFlatJson(e))))
    }
  }

  def getPerson(id: Long) = Action {
    try {
      val entity = database.entity(id)
      Ok(Json.obj(
        "result" -> "OK",
        "data"   -> Json.toJson(entity)
      ))
    } catch {
      case _ =>
        BadRequest(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found"))
    }
  }

  def getPerson2(id: Long) = Action {
    try {
      val entity = database.entity(id)
      Ok(Json.obj(
        "result" -> "OK",
        "data"   -> Json.toJson(entity)(Person.jsonWrites)
      ))
    } catch {
      case t: Throwable =>
        BadRequest(t.getMessage)
        // BadRequest(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found"))
    }
  }

  // please note the selective update
  def updatePerson(id: Long) = Action(parse.json) { request =>
    val json = request.body

    // validates Json format and then converts it into Datomic Entity
    json.validate(
      (((__ \ 'name)      .json.pickBranch keepAnd (__ \ 'name)      .read[String]) and
       ((__ \ 'age)       .json.pickBranch keepAnd (__ \ 'age)       .read[Long])   and
       ((__ \ 'dog)       .json.pickBranch keepAnd (__ \ 'dog)       .read[String]) and
       ((__ \ 'characters).json.pickBranch keepAnd (__ \ 'characters).read[Set[String]])
      ).reduce.andThen(Person.jsonReads)
    ) map { partialEntity =>
      Async {
        Datomic.transact( Entity.add(DId(id), partialEntity) ) map { tx =>
          Ok(Json.obj("result" -> "OK", "id" -> tx.toString))
        }
      }
    } recoverTotal { errors =>
      BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) ))
    }
  }
}
