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

import reactivedatomic.Datomic._ 
import reactivedatomic._
import reactivedatomic.EntityImplicits._ 

import play.modules.datomic._
import play.modules.datomic.Implicits._

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
        Datomic.typed.addToEntity(dogId)(Props(
          Dog.name -> name
        ))
      ).map{ tx =>
        // resolves real ID
        tx.resolve(dogId).map{ realid: DLong =>
          Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realid.underlying)))
        }.getOrElse(
          BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> "unable to resolve Id")))
        )
      }
    }
  }

  def getDog(id: Long) = Action {
    database.entity(DLong(id)).map{ entity =>
      Ok(Json.obj(
        "result" -> "OK", 
        "data" -> Json.toJson(entity)(Dog.jsonWrites)
      ))
    }.getOrElse(
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"unable to resolve enttiy with id:$id")))
    )
  }

  def insertPerson = Action(parse.json) { request =>
    val json = request.body

    json.validate(Person.jsonReads).map{ partialEntity =>
      val personId = DId(Common.MY_PART)
      Async {
        Datomic.transact( Datomic.addToEntity(personId, partialEntity) ).map{ tx => 
          tx.resolve(personId).map{ realId =>
            Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId.as[Long])))
          }.getOrElse(
            BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> "unable to resolve inserted person entity")))
          )
        }
      }
    }.recover{ e =>
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> JsError.toFlatJson(e))))
    }
  }

  def getPerson(id: Long) = Action {
    database.entity(id).map{ entity =>
      Ok(
        Json.obj(
          "result" -> "OK", 
          "data" -> Json.toJson(entity)
        )
      )
    }.getOrElse{
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found")))
    }
  }

  def getPerson2(id: Long) = Action {
    database.entity(id).map{ entity =>
      Ok(
        Json.obj(
          "result" -> "OK", 
          "data" -> Json.toJson(entity)(Person.jsonWrites)
        )
      )
    }.getOrElse{
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found")))
    }
  }

  // please note the selective update
  def updatePerson(id: Long) = Action(parse.json) { request =>
    val json = request.body

    // validates Json format and then converts it into Datomic Entity
    json.validate(
      (((__ \ 'name).json.pickBranch keepAnd (__ \ 'name).read[String]) and
       ((__ \ 'age).json.pickBranch keepAnd (__ \ 'age).read[Long]) and
       ((__ \ 'dog).json.pickBranch keepAnd (__ \ 'dog).read[String]) and
       ((__ \ 'characters).json.pickBranch keepAnd (__ \ 'characters).read[Set[String]])
      ).reduce.andThen(Person.jsonReads)
    ).map{ partialEntity =>
      Async {
        Datomic.transact( Datomic.addToEntity(DId(id), partialEntity) ).map{ tx => 
          Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> tx.toString)))
        }
      }
    }.recover{ errors => BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )) }
  }
}