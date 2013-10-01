package models

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

import datomisca._
import DatomicMapping._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.modules.datomisca.Implicits._
import play.api.data.validation.ValidationError

object Common {
  // the partition in which I'll store data
  // USER is not advised apparenty in PROD
  val MY_PART = Partition(Keyword("my_part"))
}

object Dog {
  val dog = Namespace("dog")

  val name = Attribute(dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")
  val schema = Seq(name)


  val queryDogByName = Query("""
    [ :find ?e :in $ ?name :where [?e :dog/name ?name] ]
  """)

  def getIdByName(dogName: String)(implicit database: DDatabase): Option[Long] = {
    Datomic.q(queryDogByName, database, DString(dogName)).headOption map {
      case dogId: DLong => dogId.underlying
    }
  }  

  // Json Reads/Writes
  val jsonWrites = (__ \ "name").write( writeAttr(Dog.name) )
}


/** PERSON */

object Person {
  // Namespaces
  val person = new Namespace("person") {
    val characters = Namespace("person.characters")
  }

  // Attributes
  val name       = Attribute(person / "name",       SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age        = Attribute(person / "age",        SchemaType.long,   Cardinality.one).withDoc("Person's age")
  val dog        = Attribute(person / "dog",        SchemaType.ref,    Cardinality.one).withDoc("Person's dog")
  val characters = Attribute(person / "characters", SchemaType.ref,    Cardinality.many).withDoc("Person's characters")

  // Characters
  val violent = AddIdent(person.characters / "violent")
  val weak    = AddIdent(person.characters / "weak")
  val clever  = AddIdent(person.characters / "clever")
  val dumb    = AddIdent(person.characters / "dumb")
  val stupid  = AddIdent(person.characters / "stupid")

  // Schema
  val schema = Seq(
    name, age, dog, characters,
    violent, weak, clever, dumb, stupid
  )

  // Json Reads/Writes
  // this one is a bit special as it searches for Dog in the DB to link it to Person
  def jsonReads(implicit database: DDatabase): Reads[PartialAddEntity] = (
    (__ \ 'name).read(readAttr[String](Person.name)) and
    (__ \ 'age) .read(readAttr[Long](Person.age))  and
    (__ \ 'dog) .read(
      // retrieves the dog from DB
      Reads.of[String]
        .map{ dogName => 
          Dog.getIdByName(dogName) map { id =>
            // creates a PartialAddToEntity
            Props( Person.dog -> DRef(DId(id)) ).convert
          }
        }
        .filter(ValidationError("datomic.entity.not.found")){ _.isDefined }
        .map{ _.get }
    ) and
    // need to specify type because a ref/many can be a list of dref or entities so need to tell it explicitly
    (__ \ 'characters).read( readAttr[Set[DRef]](Person.characters) )
    reduce
  )

  def jsonWrites = (
    (__ \ "name")      .write(writeAttr[String](Person.name)) and
    (__ \ "age")       .write(writeAttr[Long](Person.age))  and
    (__ \ "dog")       .write(writeAttr[DRef](Person.dog))  and
    (__ \ "characters").write(writeAttr[Set[DRef]](Person.characters))
    join
  )
}