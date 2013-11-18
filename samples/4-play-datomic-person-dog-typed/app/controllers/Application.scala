package controllers


import scala.concurrent._
import scala.concurrent.duration.Duration

import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads.JsObjectReducer
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import datomisca._

import play.modules.datomisca._
import play.modules.datomisca.Implicits._

import models._


object Application extends Controller {
  val uri = DatomicPlugin.uri("mem")
  implicit val conn = Datomic.connect(uri)

  def index = Action {
    Ok("Ok")
  }

  def insertDog(name: String) = Action.async {
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

  def getDog(id: Long) = Action {
    try {
      val entity = conn.database.entity(id)
      Ok(Json.obj(
        "result" -> "OK",
        "data"   -> Json.toJson(entity)(Dog.jsonWrites)
      ))
    } catch {
      case _ : Throwable =>
        BadRequest(Json.obj("result" -> "KO", "error" -> s"unable to resolve enttiy with id:$id"))
    }
  }

  def insertPerson = Action.async(parse.json) { request =>
    val json = request.body

    implicit val database = conn.database

    json.validate(Person.jsonReads) map { partialEntity =>
      val personId = DId(Common.MY_PART)
      Datomic.transact( Entity.add(personId, partialEntity) ) map { tx =>
        val realId = tx.resolve(personId)
        Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId)))          
      }
    } recoverTotal { e =>
      Future.successful(BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> JsError.toFlatJson(e)))))
    }
  }

  def getPerson(id: Long) = Action {
    try {
      val entity = conn.database.entity(id)
      Ok(Json.obj(
        "result" -> "OK",
        "data"   -> Json.toJson(entity)
      ))
    } catch {
      case _:Throwable =>
        BadRequest(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found"))
    }
  }

  def getPerson2(id: Long) = Action {
    implicit val database = conn.database
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
  def updatePerson(id: Long) = Action.async(parse.json) { request =>
    val json = request.body

    implicit val database = conn.database

    // validates Json format and then converts it into Datomic Entity
    json.validate(
      (((__ \ 'name)      .json.pickBranch keepAnd (__ \ 'name)      .read[String]) and
       ((__ \ 'age)       .json.pickBranch keepAnd (__ \ 'age)       .read[Long])   and
       ((__ \ 'dog)       .json.pickBranch keepAnd (__ \ 'dog)       .read[String]) and
       ((__ \ 'characters).json.pickBranch keepAnd (__ \ 'characters).read[Set[String]])
      ).reduce.andThen(Person.jsonReads)
    ) map { partialEntity =>
      Datomic.transact( Entity.add(DId(id), partialEntity) ) map { tx =>
        Ok(Json.obj("result" -> "OK", "id" -> tx.toString))
      }
    } recoverTotal { errors =>
      Future.successful(BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )))
    }
  }
}
