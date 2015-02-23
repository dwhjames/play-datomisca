package controllers

import scala.language.reflectiveCalls
import scala.concurrent.Future
import scala.io.Source

import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import datomisca.plugin.DatomiscaPlayPlugin
import datomisca._


object Application extends Controller {

  val community = new Namespace("community") {
    val tpe = new Namespace("community.type")
  }

  val uri = DatomiscaPlayPlugin.uri("seattle")
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
        case eid: Long =>
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
        case eid: Long =>
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
        case eid: Long =>
          val entity = database.entity(eid)
          val neighborhood = entity.as[Entity](community / "neighborhood")
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
        case eid: Long =>
          val entity = database.entity(eid)
          val neighborhood = entity.as[Entity](community / "neighborhood")
          val communities = neighborhood.as[Set[Entity]](community / "_neighborhood")
          val names = communities map { comm => comm.as[String](community / "name") }
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
        case (eid: Long, name: String) => name
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
        case (name: String, url: String) => Json.obj("name" -> name, "url" -> url)
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
        case (e: Long, c: String) => Json.obj("id" -> e, "url" -> c)
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
        case (e: String) => e
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
        case (e: String) => e
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
        case (cname: String, rname: Keyword) => Json.obj(cname -> rname.toString)
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

    val res1 = Datomic.q(query, database, (community.tpe / "twitter"))
    val res2 = Datomic.q(query, database, (community.tpe / "facebook-page"))
    Ok(Json.toJson(
      (res1 ++ res2) map {
        case (name: String) => name
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
        Seq(":community.type/facebook-page", ":community.type/twitter")
      ) map {
          case (cname: String, tpe: String) => Json.obj(cname -> tpe)
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
      Seq(
        Seq(":community.type/email-list", ":community.orgtype/community"),
        Seq(":community.type/website", ":community.orgtype/commercial")
      )
    )

    Ok(Json.toJson(
      results map {
        case (cname: String, tpe: String, orgType: String) => Json.arr(cname, tpe, orgType)
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
        case (cname: String) => Json.obj("community" -> cname)
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
        case (cname: String) => Json.obj("community" -> cname)
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

    val results = Datomic.q(query, database, ":community.type/website", "food")

    Ok(Json.toJson(
      results map {
        case (cname: String, cat: String) => Json.obj(cname -> cat)
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
        case (cname: String) => Json.obj("community" -> cname)
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
        case (cname: String) => Json.obj("community" -> cname)
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
        case (cname: String) => Json.obj("community" -> cname)
      }
    ))
  }

  def findAllDBTx = Action.async {
    val database = conn.database
    val query = Query("""
      [:find ?when :where [?tx :db/txInstant ?when]]
    """)

    val results = Datomic.q(query, database)

    val sorted = results.map( dd => dd.asInstanceOf[java.util.Date]).toSeq.sortWith( (_1: java.util.Date, _2: java.util.Date) => _1.after(_2) )
    val data_tx_date = sorted(0)
    val schema_tx_date = sorted(1)

    val query2 = Query("""
      [:find ?c :where [?c :community/name]]
    """)

    val nbSch = Datomic.q(query2, database.asOf(schema_tx_date)).size
    val nbData = Datomic.q(query2, database.asOf(data_tx_date)).size

    val dataContent = Source.fromInputStream(current.resourceAsStream("seattle-data1.edn").get).mkString
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
            "data"              -> data_tx_date.getTime,
            "schema"            -> schema_tx_date.getTime,
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


  private val communitiesPartition = new Partition(Namespace.DB.PART / "communities")

  def makeNewPartition = Action.async {

    val partitionTx: TxData = Fact.partition(communitiesPartition)

    Datomic.transact(partitionTx) map { tx =>

      Ok(Json.obj("tx" -> tx.toString))
    }
  }

  def makeNewComm = Action.async {
    val newComm = Entity.add(DId(communitiesPartition))(
      community / "name" -> "Easton"
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

    val id = Datomic.q(query, database).head.asInstanceOf[Long]

    val op = Fact.add(DId(id))( community / "category" -> "free stuff" )
    Datomic.transact(op) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }

  }

  def retractComm = Action.async {
    val database = conn.database
    val query = Query("""
      [:find ?id :where [?id :community/name "belltown"]]
    """)

    val id = Datomic.q(query, database).head.asInstanceOf[Long]

    val op = Entity.retract(id)
    Datomic.transact(op) map { tx =>
      Ok(Json.obj("tx" -> tx.toString))
    }

  }

  def transReport = Action.async {
    val newComm = Entity.add(DId(communitiesPartition))(
      community / "name" -> "Easton"
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
            Datomic.q(query, report.dbAfter, report.txData).map{
              case (e, aname, v, added) =>
                Json.arr(e.toString, aname.toString, v.toString, added.toString)
            }.foldLeft(Json.arr())( (acc, e) => acc :+ e)
          }
      }

    }
  }
}
