package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

import play.api.Play.current
import play.api.libs.json._
import play.api.libs.functional.syntax._

import datomisca._
import Datomic._ 
import play.modules.datomisca._

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
        Entity.add(dogId)(
          Dog.dog / "name" -> name
        )
      ) map { tx =>
        // resolves real ID
        val Some(realId) = tx.resolve(dogId)
        Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId.underlying)))
      }
    }
  }

  def getDog(id: Long) = Action {
    Try {
      val entity = database.entity(id)
      Ok(Json.obj(
        "result" -> "OK", 
        "dog" -> Json.obj(
          "name" -> entity.getAs[String](Dog.dog / "name")
        )
      ))
    } getOrElse {
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"unable to resolve enttiy with id:$id")))
    }
  }

  val queryDogByName = Query("""
    [ :find ?e :in $ ?name :where [?e :dog/name ?name] ]
  """)


  def insertPerson = Action(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name)      .read[String] and
      (__ \ 'age)       .read[Long]   and
      (__ \ 'dog)       .read[String] and
      (__ \ 'characters).read[Set[String]]
      tupled
    ) map {
      case (name, age, dogName, characters) =>
        Datomic.q(queryDogByName, database, DString(dogName)).headOption map {
          case dogId: DLong =>
            val personId = DId(Common.MY_PART)
            Async {
              Datomic.transact(
                Entity.add(personId)(
                  Person.person / "name"       -> name,
                  Person.person / "age"        -> age,
                  Person.person / "dog"        -> DRef(DId(dogId)),
                  Person.person / "characters" -> (characters map { ch => DRef( Person.person.characters / ch ) })
                )
              ) map { tx =>
                val Some(realId) = tx.resolve(personId)
                Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId.as[Long])))                
              }
            }
        } getOrElse {
          BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Dog with name $dogName not found")))
        }
      case _ => BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"unexpected result")))
    } recoverTotal { errors =>
      BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) ))
    }
  }

  def getPerson(id: Long) = Action {
    try {
      val entity = database.entity(id)
      println("dog:"+ entity(Person.person / "dog").as[DEntity].toMap)
      println("dogName:"+entity.as[DEntity](Person.person / "dog").as[String](Dog.dog / "name"))
      println("chars:"+entity(Person.person / "characters").as[Set[String]])
      val name = entity(Person.person / "name").as[String]
      val age  = entity(Person.person / "age").as[Long]
      val dog  = entity(Person.person / "dog").as[DEntity]
      val dogName = dog(Dog.dog / "name").as[String]
      val characters = entity(Person.person / "characters").as[Set[DRef]].map(_.toString)
      Ok(Json.obj(
        "result" -> "OK", 
        "dog" -> Json.obj(
          "name" -> name,
          "age" -> age,
          "dog" -> dogName,
          "characters" -> Json.toJson(characters)
        )))
    } catch {
      case e: EntityNotFoundException =>
        BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found")))
      case e: Throwable =>
        BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Person entity not mappable")))
    }
  }

  // please note the selective update
  def updatePerson(id: Long) = Action(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name).readNullable[String] and
      (__ \ 'age).readNullable[Long] and
      (__ \ 'dog).readNullable[String] and
      (__ \ 'characters).readNullable[Set[String]]
      tupled
    ) map {
      case (name: Option[String], age: Option[Long], dogName: Option[String], characters: Option[_]) =>
        val builder = Map.newBuilder[Keyword, DatomicData]
        name foreach { name => builder += (Person.person / "name" -> DString(name)) }
        age  foreach { age  => builder += (Person.person / "age"  -> DLong(age)) }
        dogName foreach { dogName =>
          Datomic.q(queryDogByName, database, DString(dogName)).headOption.foreach{ 
            case dogid: DLong => builder += (Person.person / "dog" -> DRef(DId(dogid)))
            case _ =>
          }
        }
        characters foreach { characters =>
          builder += (Person.person / "characters" -> new DSet(characters.map( ch => DRef(Person.person.characters / ch)) ))
        }
        
        Async {
          Datomic.transact(Entity.add(DId(id), builder.result)) map { tx =>
            Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> tx.toString)))
          }
        }
    } recoverTotal { errors =>
      BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) ))
    }
  }
}