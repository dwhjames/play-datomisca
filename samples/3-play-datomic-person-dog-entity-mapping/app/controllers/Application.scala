package controllers

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}

import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.functional.syntax._

import datomisca._
import play.modules.datomisca._

import models._


object Application extends Controller {
  val uri = DatomicPlugin.uri("mem")
  implicit val conn = Datomic.connect(uri)

  def index = Action {
    Ok("Ok")
  }

  def insertDog(name: String) = Action.async {
    Dog.insert(Dog(name)).map { realid =>
      Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realid)))
    }
  }

  def getDog(id: Long) = Action {
    Dog.get(id) map { dog =>
      Ok(Json.obj("result" -> "OK", "dog" -> dog.toString))
    } recover {
      case e: Exception => BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> e.getMessage)))
    } get
  }

  def insertPerson = Action.async(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name).read[String] and
      (__ \ 'age).read[Long] and
      (__ \ 'dog).read[String] and
      (__ \ 'characters).read[Set[String]]
      tupled
    ).map {
      case (name, age, dogName, characters) =>
        Dog.find(dogName) match {
          case Some((id, dog)) =>
            val person = Person(
              name, 
              age, 
              IdView(id.underlying)(dog), // a reference to 
              characters map { ch => DRef( Person.person.characters / ch ) }
            )
            Person.insert(person) map { realid =>
              Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realid)))
            }
          case _ => Future.successful(BadRequest(Json.obj("result" -> "KO", "errors" -> s"dog $dogName not found")))
        }
    } recoverTotal { errors =>
      Future.successful(BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )))
    }
  }

  def getPerson(id: Long) = Action {
    Person.get(id) map { person =>
      Ok(Json.obj("result" -> "OK", "person" -> person.toString))
    } recover {
      case e: Exception => BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> e.getMessage)))
    } get
  }

  // bad update as it updates everything and not only the changed fields(which makes case class not cool for updates)
  def updatePerson(id: Long) = Action.async(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name).read[String] and
      (__ \ 'age).read[Long] and
      (__ \ 'dog).read[String] and
      (__ \ 'characters).read[Set[String]]
      tupled
    ) map {
      case (name: String, age: Long, dogName: String, characters: Set[_]) =>
        (Person.get(id), Dog.find(dogName)) match {
          case (Success(person), Some((did, dog))) => 
            val person = Person(
              name, 
              age, 
              IdView(did.underlying)(dog), // a reference to 
              characters map { ch => DRef( Person.person.characters / ch ) }
            )
            Person.update(id, person) map { tx =>
              Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> tx.toString)))
            }
          case _ => Future.successful(BadRequest(Json.obj("result" -> "KO", "errors" -> s"person with $id or dog $dogName not found")))
        }
    } recoverTotal { errors =>
      Future.successful(BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )))
    }
  }
}
