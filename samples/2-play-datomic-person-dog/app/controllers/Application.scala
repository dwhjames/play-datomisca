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

import reactivedatomic.Datomic._ 
import reactivedatomic._
import play.modules.datomic._

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
        Datomic.addToEntity(dogId)(
          Dog.dog / "name" -> name
        )
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
        "dog" -> Json.obj(
          "name" -> entity.getAs[DString](Dog.dog / "name").as[String]
        )
      ))
    }.getOrElse(
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"unable to resolve enttiy with id:$id")))
    )
  }

  val queryDogByName = Datomic.typedQuery[Args2, Args1]("""
    [ :find ?e :in $ ?name :where [?e :dog/name ?name] ]
  """)

  def insertPerson = Action(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name).read[String] and
      (__ \ 'age).read[Long] and
      (__ \ 'dog).read[String] and
      (__ \ 'characters).read[Set[String]]
      tupled
    ).map {
      case (name, age, dogName, characters) =>
        Datomic.query(queryDogByName, database, DString(dogName)).headOption.map{ 
          case dogId: DLong =>
            val personId = DId(Common.MY_PART)
            Async {
              Datomic.transact(
                Datomic.addToEntity(personId)(
                  Person.person / "name" -> name,
                  Person.person / "age" -> age,
                  Person.person / "dog" -> DRef(DId(dogId)),
                  Person.person / "characters" -> characters.map{ ch => DRef( Person.person.characters / ch ) } 
                )
              ).map{ tx => 
                tx.resolve(personId).map{ realId =>
                  Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> realId.as[Long])))
                }.getOrElse(
                  BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"unable to resolve person entity with id:$personId")))
                )
              }
            }
        }.getOrElse(
          BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Dog with name $dogName not found")))
        )
    }.recover{ errors => BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )) }
  }

  def getPerson(id: Long) = Action {
    database.entity(id).map{ entity =>
      Ok(Json.obj(
        "result" -> "OK", 
        "dog" -> (for{
          name <- entity.tryGetAs[DString](Person.person / "name")
          age <- entity.tryGetAs[DLong](Person.person / "age")
          dog <- entity.tryGetAs[DEntity](Person.person / "dog")
          dogName <- dog.tryGetAs[DString](Dog.dog / "name")
          characters <- entity.tryGetAs[DSet](Person.person / "characters")
        } yield( Json.obj(
          "name" -> name.as[String],
          "age" -> age.as[Long],
          "dog" -> dogName.as[String],
          "characters" -> characters.toSet.map(_.toString)
        ))).get
      ))
    }.getOrElse{
      BadRequest(Json.toJson(Json.obj("result" -> "KO", "error" -> s"Person with id $id not found")))
    }
  }

  // please note the selective update
  def updatePerson(id: Long) = Action(parse.json) { request =>
    val json = request.body

    json.validate(
      (__ \ 'name).readOpt[String] and
      (__ \ 'age).readOpt[Long] and
      (__ \ 'dog).readOpt[String] and
      (__ \ 'characters).readOpt[Set[String]]
      tupled
    ).map{
      case (name: Option[String], age: Option[Long], dogName: Option[String], characters: Option[Set[_]]) =>
        val builder = Map.newBuilder[Keyword, DatomicData]
        name.foreach( name => builder += (Person.person / "name" -> DString(name)) )
        age.foreach( age => builder += (Person.person / "age" -> DLong(age)) )
        dogName.foreach{ dogName =>
          Datomic.query(queryDogByName, database, DString(dogName)).headOption.foreach{ 
            case dogid: DLong => builder += (Person.person / "dog" -> DRef(DId(dogid)))
          }
        }
        characters.foreach{ characters =>
          builder += (Person.person / "characters" -> new DSet(characters.map( ch => DRef(Person.person.characters / ch)) ))
        }
        
        Async{
          Datomic.transact(AddToEntity(DId(id), builder.result)).map{ tx =>
            Ok(Json.toJson(Json.obj("result" -> "OK", "id" -> tx.toString)))
          }
        }
    }.recover{ errors => BadRequest(Json.obj("result" -> "KO", "errors" -> JsError.toFlatJson(errors) )) }
  }
}