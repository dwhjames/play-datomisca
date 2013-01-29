package models

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

import datomisca._
import Datomic._

object Common {
  // the partition in which I'll store data
  // USER is not advised apparenty in PROD
  val MY_PART = Partition(Keyword("my_part"))
}

object Dog {
  val dog = new Namespace("dog")

  val name = Attribute( dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")

  val schema = Seq(name)
}


/** PERSON */

object Person {
  // Namespaces
  val person = new Namespace("person") {
    val characters = Namespace("person.characters")
  }

  // Attributes
  val name       = Attribute(person / "name",       SchemaType.string, Cardinality.one) .withDoc("Person's name")
  val age        = Attribute(person / "age",        SchemaType.long,   Cardinality.one) .withDoc("Person's age")
  val dog        = Attribute(person / "dog",        SchemaType.ref,    Cardinality.one) .withDoc("Person's dog")
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

}