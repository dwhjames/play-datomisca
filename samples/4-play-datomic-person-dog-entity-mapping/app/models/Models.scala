package models

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

import reactivedatomic._
import Datomic._
import EntityImplicits._

object Common {
  // the partition in which I'll store data
  // USER is not advised apparenty in PROD
  val MY_PART = Partition(Keyword("my_part"))
}

//////////////////////////////////////////////////////////////////////////////////////////////
// Please not that case-class mapping is a facility on top of datomic tools.
// But it's nothing more than a conversion of a case class to several Add ops
// It doesn't manage, cache or do anything else so you must manage everything
// by yourself.

/** DOG */
case class Dog(name: String)

object Dog {
  val dog = new Namespace("dog")

  val name = Attribute( dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")
  
  val schema = Seq(name)

  // the special case of a 1 single field case class
  implicit val dogReader = name.read[String].map( Dog(_) )
  implicit val dogWriter = name.write[String].contramap{ d: Dog => d.name }

  def get(id: Long)(implicit conn: Connection): Try[Dog] = {
    database.entity(DLong(id))
            .map{ entity =>           
              play.Logger.info("entity:"+entity.entity.keySet)
              fromEntity[Dog](entity) }
            .getOrElse(Failure(new RuntimeException("Entity not found")))
  }

  def insert(dog: Dog)(implicit conn: Connection, ex: ExecutionContext): Future[Long] = {
    val tempid = DId(Common.MY_PART)
    val entity = toEntity(tempid)(dog)

    Datomic.transact(toEntity(tempid)(dog)).flatMap{ tx =>
      tx.resolve(tempid)
        .map{ dl => Future.successful(dl.value) }
        .getOrElse(Future.failed(new RuntimeException("failed to resolve tempid")))
    }
  }

  // not "remove" because nothing is removed in Datomic
  def retract(id: Long)(implicit conn: Connection, ex: ExecutionContext): Future[TxReport] = {
    Datomic.transact(retractEntity(DLong(id)))
  }

  def update(id: Long, dog: Dog)(implicit conn: Connection, ex: ExecutionContext): Future[TxReport] = {
    Datomic.transact( toEntity(DId(id))(dog) )
  }

  def find(name: String)(implicit conn: Connection): Option[(DLong, Dog)] = {
    val q = Datomic.typedQuery[Args2, Args1]("""
      [ 
        :find ?e
        :in $ ?name
        :where [ ?e :dog/name ?name ]
      ]
    """)

    Datomic.query(q, database, DString(name)).headOption.flatMap{
      case e: DLong =>
        database.entity(e).flatMap{ entity =>
          fromEntity[Dog](entity).toOption
        }.map( dog => e -> dog )
      case _ => throw new RuntimeException("unexpected result")
    }
  }
}


/** PERSON */

// Ref is a pure technical class used to indicate that it references another entity (also contains the DId temporary or final)
// DRef is a direct reference to a pure non-typed Ident (an enumerator in datomic)
case class Person(name: String, age: Long, dog: Ref[Dog], characters: Set[DRef])

object Person {
  import Dog._

  // Namespaces
  val person = new Namespace("person") {
    val characters = Namespace("person.characters")
  }

  // Attributes
  val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's age")
  val dog = Attribute( person / "dog", SchemaType.ref, Cardinality.one).withDoc("Person's dog")
  val characters = Attribute( person / "characters", SchemaType.ref, Cardinality.many).withDoc("Person's characterS")

  // Characters
  val violent = AddIdent(person.characters / "violent")
  val weak = AddIdent(person.characters / "weak")
  val clever = AddIdent(person.characters / "clever")
  val dumb = AddIdent(person.characters / "dumb")
  val stupid = AddIdent(person.characters / "stupid")

  // Schema
  val schema = Seq(
    name, age, dog, characters,
    violent, weak, clever, dumb, stupid
  )

  // entity mappers
  implicit val personReader = (
    name.read[String] and 
    age.read[Long] and
    dog.read[Ref[Dog]] and
    characters.read[Set[DRef]]
  )(Person.apply _)

  implicit val personWriter = (
    name.write[String] and 
    age.write[Long] and
    dog.write[Ref[Dog]] and
    characters.write[Set[DRef]]
  )(unlift(Person.unapply))

  /** 
    * Ops (implicit connection is the basic requirement for everything such as getting database)
    */  
  def all()(implicit conn: Connection): Try[List[Person]] = {
    // query with 0 Input and 1 Output
    val q = Datomic.typedQuery[Args0, Args1]("""
      [ :find ?e :where [ ?e :person/name ] ]
    """)

    Utils.sequence(Datomic.query(q).collect{ 
      case e: DLong =>
        database.entity(e).map{ entity =>
          fromEntity[Person](entity)
        }.getOrElse(Failure(new RuntimeException("Entity not found")))
    })
  }

  def get(id: Long)(implicit conn: Connection): Try[Person] = {
    database.entity(DLong(id))
            .map{ entity => fromEntity[Person](entity) }
            .getOrElse(Failure(new RuntimeException("Entity not found")))
  }

  def insert(person: Person)(implicit conn: Connection, ex: ExecutionContext): Future[Long] = {
    val tempid = DId(Common.MY_PART)
    val entity = toEntity(tempid)(person)

    Datomic.transact(toEntity(tempid)(person)).flatMap{ tx =>
      tx.resolve(tempid)
        .map{ dl => Future.successful(dl.value) }
        .getOrElse(Future.failed(new RuntimeException("failed to resolve tempid")))
    }
  }

  // not "remove" because nothing is removed in Datomic
  def retract(id: Long)(implicit conn: Connection, ex: ExecutionContext): Future[TxReport] = {
    Datomic.transact(retractEntity(DLong(id)))
  }

  def update(id: Long, person: Person)(implicit conn: Connection, ex: ExecutionContext): Future[TxReport] = {
    Datomic.transact( toEntity(DId(id))(person) )
  }

  def find(name: String)(implicit conn: Connection): Option[(DLong, Person)] = {
    val q = Datomic.typedQuery[Args2, Args1]("""
      [ 
        :find ?e
        :in $ ?name
        :where [ ?e :person/name ?name ]
      ]
    """)

    Datomic.query(q, database, DString(name)).headOption.flatMap{
      case e: DLong =>
        database.entity(e).flatMap{ entity =>
          fromEntity[Person](entity).toOption
        }.map( person => e -> person )
      case _ => throw new RuntimeException("unexpected result")
    }
  }
}