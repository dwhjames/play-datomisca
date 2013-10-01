package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}
import scala.io.Source

import datomisca._

import play.modules.datomisca._

import play.api.libs.json._
import play.api.libs.json.Json

import scala.language.reflectiveCalls

object Application extends Controller {
  implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val community = new Namespace("community") {
    val tpe = new Namespace("community.type")
  }

  val uri = DatomicPlugin.uri("seattle")
  implicit val conn = Datomic.connect(uri)

  def countCommunities = Action {
    val query = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    val sz = Datomic.q(query, conn.database).size

    Ok("Found %d communities".format(sz))

  }

  def getFirstEntity = Action {
    val database = conn.database
    val query = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    try {
      Datomic.q(query, database).headOption map {
        case eid: DLong =>
          val e = database.entity(eid)
          Ok(Json.toJson(e.toMap map { case (k, v) => k.toString -> v.toString }))
      } getOrElse {
        NotFound("No communities")
      }
    } catch {
      case e: Throwable => InternalServerError(e.getMessage)
    }
  }

  def getCommunityNames = Action {
    val database = conn.database

    val query = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    try {
      val l = Datomic.q(query, database) map {
        case eid: DLong =>
          val entity = database.entity(eid)
          val name = entity.as[String](community / "name")
          name
      }
      Ok(Json.toJson(l))
    } catch {
      case e: Throwable => InternalServerError(e.getMessage)
    }
  }

  def getCommunityNeighborHoods = Action {
    val database = conn.database

    val query = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    try {
      val l = Datomic.q(query, database) map {
        case eid: DLong =>
          val entity = database.entity(eid)
          val neighborhood = entity(community / "neighborhood").as[DEntity]
          neighborhood.toMap map { case (k, v) => k.toString -> v.toString }
      }
      Ok(Json.toJson(l))
    } catch {
      case e: Throwable => InternalServerError(e.getMessage)
    }
  }

  def getNeighborHoodCommunities = Action {
    val database = conn.database

    val query = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    try {
      Datomic.q(query, database).headOption map {
        case eid: DLong =>
          val entity = database.entity(eid)
          val neighborhood = entity(community / "neighborhood").as[DEntity]
          val communities = neighborhood(community / "_neighborhood").as[Set[DEntity]]
          val names = communities map { comm => comm(community / "name").as[String] }
          Ok(Json.toJson(names))
      } getOrElse {
        NotFound("no community found")
      }
    } catch {
      case e: Throwable => InternalServerError(e.getMessage)
    }
  }

  def findAllCommunityNames = Action {
    val database = conn.database
    val query = Query("""
      [:find ?c ?n :where [?c :community/name ?n]]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (eid: DLong, name: DString) => name.as[String]
      }
    ))
  }

  def findAllCommunityNamesAndUrls = Action {
    val database = conn.database
    val query = Query("""
      [:find ?n ?u :where [?c :community/name ?n][?c :community/url ?u]]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (name: DString, url: DString) => Json.obj("name" -> name.as[String], "url" -> url.as[String])
      }
    ))
  }

  def findCategoriesForBelltown = Action {
    val database = conn.database
    val query = Query("""
      [:find ?e ?c :where [?e :community/name "belltown"][?e :community/category ?c]]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (e: DLong, c: DString) => Json.obj("id" -> e.as[Long], "url" -> c.as[String])
      }
    ))
  }

  def findTwitterCommunities = Action {
    val database = conn.database
    val query = Query("""
      [:find ?n :where [?c :community/name ?n][?c :community/type :community.type/twitter]]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (e: DString) => e.as[String]
      }
    ))
  }

  def findNECommunities = Action {
    val database = conn.database
    val query = Query("""
      [:find ?c_name
       :where [?c :community/name ?c_name]
              [?c :community/neighborhood ?n]
              [?n :neighborhood/district ?d]
              [?d :district/region :region/ne]
      ]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (e: DString) => e.as[String]
      }
    ))
  }

  def findCommunitiesRegions = Action {
    val database = conn.database
    val query = Query("""
      [:find ?c_name ?r_name
      :where [?c :community/name ?c_name]
             [?c :community/neighborhood ?n]
             [?n :neighborhood/district ?d]
             [?d :district/region ?r]
             [?r :db/ident ?r_name]]
    """)

    Ok(Json.toJson(
      Datomic.q(query, database) map {
        case (cname: DString, rname: DRef) => Json.obj(cname.as[String] -> rname.toString)
      }
    ))
  }

  def findTwitterFacebook = Action {
    val database = conn.database
    val query = Query("""
      [
       :find ?n
       :in $ ?t
       :where [?c :community/name ?n]
             [?c :community/type ?t]
      ]
    """)

    val res1 = Datomic.q(query, database, DRef(community.tpe / "twitter"))
    val res2 = Datomic.q(query, database, DRef(community.tpe / "facebook-page"))
    Ok(Json.toJson(
      (res1 ++ res2) map {
        case (name: DString) => name.as[String]
      }
    ))
  }

  def findTwitterFacebookList = Action {
    val database = conn.database  
    val query = Query("""
      [
       :find ?n ?t
       :in $ [?t ...]
       :where [?c :community/name ?n]
              [?c :community/type ?t]
      ]
    """)

    Ok(Json.toJson(
      Datomic.q(
        query,
        database,
        Datomic.coll(":community.type/facebook-page", ":community.type/twitter")
      ) map {
          case (cname: DString, tpe: DString) => Json.obj(cname.as[String] -> tpe.as[String])
      }
    ))
  }

  def findNonCommercialEmaillistOrCommercialWebSites = Action {
    val database = conn.database
    val query = Query("""
      [
       :find ?n ?t ?ot
       :in $ [[?t ?ot]]
       :where [?c :community/name ?n]
              [?c :community/type ?t]
              [?c :community/orgtype ?ot]
      ]
    """)

    val results = Datomic.q(
      query, database,
      Datomic.coll(
        Datomic.coll(":community.type/email-list", ":community.orgtype/community"),
        Datomic.coll(":community.type/website", ":community.orgtype/commercial")
      )
    )

    Ok(Json.toJson(
      results map {
        case (cname: DString, tpe: DString, orgType: DString) => Json.arr(cname.as[String], tpe.as[String], orgType.as[String])
      }
    ))
  }

  def findAllCommNamesAfterCInAlphaOrder = Action {
    val database = conn.database
    val query = Query("""
      [
        :find ?n
        :where [?c :community/name ?n]
               [(.compareTo ?n "C") ?res]
               [(< ?res 0)]]
    """)

    val results = Datomic.q(query, database)

    Ok(Json.toJson(
      results map {
        case (cname: DString) => Json.obj("community" -> cname.as[String])
      }
    ))
  }

  def findAllCommWallingford = Action {
    val database = conn.database
    val query = Query("""
      [
       :find ?n
       :where [(fulltext $ :community/name "Wallingford") [[?e ?n]]]
      ]
    """)

    val results = Datomic.q(query, database)

    Ok(Json.toJson(
      results map {
        case (cname: DString) => Json.obj("community" -> cname.as[String])
      }
    ))
  }

  def findAllCommWebsitesFood = Action {
    val database = conn.database
    val query = Query("""
      [
        :find ?name ?cat
        :in $ ?type ?search
        :where [?c :community/name ?name]
               [?c :community/type ?type]
               [(fulltext $ :community/category ?search) [[?c ?cat]]]
      ]
    """)

    val results = Datomic.q(query, database, DString(":community.type/website"), DString("food"))

    Ok(Json.toJson(
      results map {
        case (cname: DString, cat: DString) => Json.obj(cname.as[String] -> cat.as[String])
      }
    ))
  }

  def findAllCommNamesTwitter = Action {
    val database = conn.database
    val rule = Query.rules("""
      [[
        [twitter ?c]
        [?c :community/type :community.type/twitter]
      ]]
    """)
    val query = Query("""
      [
        :find ?n
        :in $ %
        :where [?c :community/name ?n]
               (twitter ?c)
      ]
    """)

    val results = Datomic.q(query, database, rule)

    Ok(Json.toJson(
      results map {
        case (cname: DString) => Json.obj("community" -> cname.as[String])
      }
    ))
  }

  def findAllCommNamesNESW = Action {
    val database = conn.database
    val rule = Query.rules("""
      [[
        [region ?c ?r]
        [?c :community/neighborhood ?n]
        [?n :neighborhood/district ?d]
        [?d :district/region ?re]
        [?re :db/ident ?r]
      ]]
    """)
    val query = Query("""
      [
        :find ?n
        :in $ %
        :where [?c :community/name ?n]
               (region ?c :region/ne)
      ]
    """)

    val query2 = Query("""
      [
        :find ?n
        :in $ %
        :where [?c :community/name ?n]
               (region ?c :region/sw)
      ]
    """)

    val results = Datomic.q(query, database, rule)
    val results2 = Datomic.q(query2, database, rule)

    Ok(Json.toJson(
      (results ++  results2) map {
        case (cname: DString) => Json.obj("community" -> cname.as[String])
      }
    ))
  }

  def findAllCommNamesNESWOr = Action {
    val database = conn.database
    val rule = Query.rules("""
      [[
        [region ?c ?r]
        [?c :community/neighborhood ?n]
        [?n :neighborhood/district ?d]
        [?d :district/region ?re]
        [?re :db/ident ?r]
       ][
        [social-media ?c]
        [?c :community/type :community.type/twitter]
       ][
        [social-media ?c]
        [?c :community/type :community.type/facebook-page]
       ][
        [northern ?c] (region ?c :region/ne)
       ][
        [northern ?c] (region ?c :region/n)
       ][
        [northern ?c] (region ?c :region/nw)
       ][
        [southern ?c] (region ?c :region/sw)
       ][
        [southern ?c] (region ?c :region/s)
       ][
        [southern ?c] (region ?c :region/se)]
       ]
    """)
    val query = Query("""
      [
       :find ?n :in $ %
       :where [?c :community/name ?n]
              (northern ?c)
              (social-media ?c)
      ]
    """)

    val results = Datomic.q(query, database, rule)

    Ok(Json.toJson(
      results map {
        case (cname: DString) => Json.obj("community" -> cname.as[String])
      }
    ))
  }

  def findAllDBTx = Action.async {
    val database = conn.database
    val query = Query("""
      [:find ?when :where [?tx :db/txInstant ?when]]
    """)

    val results = Datomic.q(query, database)

    val sorted = results.map( dd => dd.asInstanceOf[DInstant]).toSeq.sortWith( (_1: DInstant, _2: DInstant) => _1.as[java.util.Date].after(_2.as[java.util.Date]) )
    val data_tx_date = sorted(0)
    val schema_tx_date = sorted(1)

    val query2 = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    val nbSch = Datomic.q(query2, database.asOf(schema_tx_date)).size
    val nbData = Datomic.q(query2, database.asOf(data_tx_date)).size

    val dataContent = Source.fromInputStream(current.resourceAsStream("seattle-data1.dtm").get).mkString
    val data = Datomic.parseOps(dataContent)

    data.map { data =>
      val dbAfter = database.withData(data).dbAfter
      val nbWithAfter = Datomic.q(query2, dbAfter).size
      val nbCurrent = Datomic.q(query2, database).size
      Datomic.transact(data) map { tx =>
        val nbTransactAfter = Datomic.q(query2, database).size
        val nbSince = Datomic.q(query2, database.since(data_tx_date)).size

        Ok(Json.toJson(
          Json.obj(
            "data"              -> data_tx_date.as[Long],
            "schema"            -> schema_tx_date.as[Long],
            "nb_schema"         -> nbSch,
            "nb_data"           -> nbData,
            "nb_after_with"     -> nbWithAfter,
            "nb_current"        -> nbCurrent,
            "nb_transact_after" -> nbTransactAfter,
            "nb_since"          -> nbSince
          )
        ))
      }
    } getOrElse {
      Future.successful(BadRequest("Data not valid"))
    }
  }

  def makeNewPartition = Action.async {
    val newPartition = Entity.add(DId(Partition.DB))(
      Namespace.DB / "ident" -> ":communities",
      Namespace.DB.INSTALL / "_partition" -> "db.part/db"
    )

    Datomic.transact(newPartition) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }
  }

  def makeNewComm = Action.async {
    val newComm = Entity.add(DId(Partition(Keyword("communities"))))(
      Namespace("community") / "name" -> "Easton"
    )

    Datomic.transact(newComm) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }
  }

  def updateComm = Action.async {
    val database = conn.database
    val query = Query("""
      [:find ?id :where [?id :community/name "belltown"]]
    """)

    val id = Datomic.q(query, database).head.asInstanceOf[DLong]

    val op = Fact.add(DId(id))( Namespace("community") / "category" -> "free stuff" )
    Datomic.transact(op) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }

  }

  def retractComm = Action.async {
    val database = conn.database
    val query = Query("""
      [:find ?id :where [?id :community/name "belltown"]]
    """)

    val id = Datomic.q(query, database).head.asInstanceOf[DLong]

    val op = Entity.retract(id)
    Datomic.transact(op) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }

  }

  def transReport = Action.async {
    val newComm = Entity.add(DId(Partition(Keyword("communities"))))(
      Namespace("community") / "name" -> "Easton"
    )

    //Await.result(Datomic.transact(newComm), Duration("2 seconds"))
    val txReportQueue = conn.txReportQueue

    Datomic.transact(newComm) map { tx =>
      play.Logger.info("hello")
      //val report = conn.connection.txReportQueue.poll()
      txReportQueue.stream.head match {
        case None => BadRequest("Unexpected result")
        case Some(report) =>
          play.Logger.info("hello2")

          val query = Query("""
            [
             :find ?e ?aname ?v ?added
             :in $ [[?e ?a ?v _ ?added]]
             :where [?e ?a ?v _ ?added]
                    [?a :db/ident ?aname]
            ]
          """)

          Ok {
            Datomic.q(query, report.dbAfter, DColl(report.txData)).map{
              case (e, aname, v, added) =>
                Json.arr(e.toString, aname.toString, v.toString, added.toString)
            }.foldLeft(Json.arr())( (acc, e) => acc :+ e)
          }
      }

    }
  }
}