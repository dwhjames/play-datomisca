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

import reactivedatomic.Datomic._ 
import reactivedatomic._
import play.modules.datomic._


import play.api.libs.json._
import play.api.libs.json.Json

object Application extends Controller {
  implicit val ctx = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val community = new Namespace("community") {
    val tpe = new Namespace("community.type")
  }

  val uri = DatomicPlugin.uri("seattle")
  implicit val conn = Datomic.connect(uri)

  def countCommunities = Action { 
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)

    val sz = Datomic.query(q).size

    Ok("Found %d communities".format(sz))
    
  }

  def getFirstEntity = Action{
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)

    Datomic.query(q).headOption.collect{ 
      case eid: DLong => 
        database.entity(eid.as[DLong]).map{ e =>
          Ok(Json.toJson(e.toMap.map{ case(k, v) => k.toString -> v.toString }))
        }.getOrElse(BadRequest("Entity not found"))
    }.getOrElse(BadRequest("Entity not found"))
    
  }

  def getCommunityNames = Action{
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)

    val l = Datomic.query(q).collect{
      case eid: DLong => 
        database.entity(eid).map{ entity =>
          entity.tryGetAs[DString](community / "name").map(Datomic.fromDatomic[String](_))
        }.get
    }
    Utils.sequence(l).map{ l =>
      Ok(Json.toJson(l))
    }.recover{
      case e: Exception => BadRequest(e.getMessage)
    }.get
  }

  def getCommunityNeighborHoods = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)

    val l = Datomic.query(q).collect{
      case eid: DLong => 
        database.entity(eid).map{ entity =>
          entity.tryGetAs[DEntity](community / "neighborhood").map(_.toMap.map{ case(k, v) => k.toString -> v.toString })
          }.get
    }
    Utils.sequence(l).map{ l =>
      Ok(Json.toJson(l))
    }.recover{
      case e: Exception => BadRequest(e.getMessage)
    }.get
  }

  def getNeighborHoodCommunities = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)
    
    Datomic.query(q).headOption.collect{ 
      case eid: DLong =>
        val communities = for{ 
          entity <- database.entity(eid)
          neighborhood <- entity.getAs[DEntity](community / "neighborhood")
          communities <- neighborhood.getAs[Set[DEntity]](community / "_neighborhood")
        } yield(communities)
        
        communities.map{ communities =>
          val names = for {
            comm <- communities
            name <- comm.getAs[String](community / "name")
          } yield(name)

          Ok(Json.toJson(names))
        }.getOrElse(BadRequest("entity not resolved"))

    }.getOrElse(BadRequest("no community found"))
    
  }

  def findAllCommunityNames = Action {
    val q=  Datomic.typedQuery[Args0, Args2]("""
      [:find ?c ?n :where [?c :community/name ?n]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).collect{
        case (eid: DLong, name: DString) => name.as[String]
      }
    ))
  }

  def findAllCommunityNamesAndUrls = Action {
    val q = Datomic.typedQuery[Args0, Args2]("""
      [:find ?n ?u :where [?c :community/name ?n][?c :community/url ?u]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).collect{
        case (name: DString, url: DString) => Json.obj("name" -> name.as[String], "url" -> url.as[String])
      }
    ))
  }

  def findCategoriesForBelltown = Action {
    val q = Datomic.typedQuery[Args0, Args2]("""
      [:find ?e ?c :where [?e :community/name "belltown"][?e :community/category ?c]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).map{
        case (e: DLong, c: DString) => Json.obj("id" -> e.as[Long], "url" -> c.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findTwitterCommunities = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?n :where [?c :community/name ?n][?c :community/type :community.type/twitter]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).map{
        case (e: DString) => e.as[String]
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findNECommunities = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c_name 
       :where [?c :community/name ?c_name]
              [?c :community/neighborhood ?n]
              [?n :neighborhood/district ?d]
              [?d :district/region :region/ne]
      ]
    """)

    Ok(Json.toJson(
      Datomic.query(q).map{
        case (e: DString) => e.as[String]
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findCommunitiesRegions = Action {
    val q = Datomic.typedQuery[Args0, Args2]("""
      [:find ?c_name ?r_name 
      :where [?c :community/name ?c_name]
             [?c :community/neighborhood ?n]
             [?n :neighborhood/district ?d]
             [?d :district/region ?r]
             [?r :db/ident ?r_name]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).map{
        case (cname: DString, rname: DRef) => Json.obj(cname.as[String] -> rname.toString)
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }
  
  def findTwitterFacebook = Action {
    val q = Datomic.typedQuery[Args2, Args1]("""
      [
       :find ?n 
       :in $ ?t 
       :where [?c :community/name ?n]
             [?c :community/type ?t]
      ]
    """)

    val res1 = Datomic.query(q, database, DRef(community.tpe / "twitter"))
    val res2 = Datomic.query(q, database, DRef(community.tpe / "facebook-page"))
    Ok(Json.toJson(
      (res1 ++ res2).map{
        case (name: DString) => name.as[String]
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findTwitterFacebookList = Action {
    val q = Datomic.typedQuery[Args2, Args2]("""
      [
       :find ?n ?t 
       :in $ [?t ...] 
       :where [?c :community/name ?n]
              [?c :community/type ?t]
      ]
    """)

    Ok(Json.toJson(
      Datomic.query(
        q, 
        database, 
        DSet(DString(":community.type/facebook-page"), DString(":community.type/twitter"))
      ).map{
          case (cname: DString, tpe: DString) => Json.obj(cname.as[String] -> tpe.as[String])
          case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findNonCommercialEmaillistOrCommercialWebSites = Action {
    val q = Datomic.typedQuery[Args2, Args3]("""
      [
       :find ?n ?t ?ot 
       :in $ [[?t ?ot]] 
       :where [?c :community/name ?n]
              [?c :community/type ?t]
              [?c :community/orgtype ?ot]
      ]
    """)

    val results = Datomic.query(
      q, database, 
      DSet(
        DSet(DString(":community.type/email-list"), DString(":community.orgtype/community")),
        DSet(DString(":community.type/website"), DString(":community.orgtype/commercial"))
      )
    )
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString, tpe: DString, orgType: DString) => Json.arr(cname.as[String], tpe.as[String], orgType.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommNamesAfterCInAlphaOrder = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [
        :find ?n 
        :where [?c :community/name ?n] 
               [(.compareTo ?n "C") ?res]
               [(< ?res 0)]]
    """)

    val results = Datomic.query(q)
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString) => Json.obj("community" -> cname.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommWallingford = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [
       :find ?n 
       :where [(fulltext $ :community/name "Wallingford") [[?e ?n]]]
      ]
    """)

    val results = Datomic.query(q)
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString) => Json.obj("community" -> cname.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommWebsitesFood = Action {
    val q = Datomic.typedQuery[Args3, Args2]("""
      [
        :find ?name ?cat 
        :in $ ?type ?search 
        :where [?c :community/name ?name]
               [?c :community/type ?type]
               [(fulltext $ :community/category ?search) [[?c ?cat]]]
      ]
    """)

    val results = Datomic.query(q, database, DString(":community.type/website"), DString("food"))
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString, cat: DString) => Json.obj(cname.as[String] -> cat.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommNamesTwitter = Action {
    val rule = Datomic.rules("""
      [[
        [twitter ?c]
        [?c :community/type :community.type/twitter]
      ]]
    """)
    val q = Datomic.typedQuery[Args2, Args1]("""
      [
        :find ?n 
        :in $ % 
        :where [?c :community/name ?n]
               (twitter ?c)
      ]
    """)

    val results = Datomic.query(q, database, rule)
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString) => Json.obj("community" -> cname.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommNamesNESW = Action {
    val rule = Datomic.rules("""
      [[
        [region ?c ?r]
        [?c :community/neighborhood ?n]
        [?n :neighborhood/district ?d]
        [?d :district/region ?re]
        [?re :db/ident ?r]
      ]]
    """)
    val q = Datomic.typedQuery[Args2, Args1]("""
      [
        :find ?n 
        :in $ % 
        :where [?c :community/name ?n]
               (region ?c :region/ne)
      ]
    """)

    val q2 = Datomic.typedQuery[Args2, Args1]("""
      [
        :find ?n 
        :in $ % 
        :where [?c :community/name ?n]
               (region ?c :region/sw)
      ]
    """)

    val results = Datomic.query(q, database, rule)
    val results2 = Datomic.query(q2, database, rule)
    
    Ok(Json.toJson(
      (results ++  results2).map{
        case (cname: DString) => Json.obj("community" -> cname.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllCommNamesNESWOr = Action {
    val rule = Datomic.rules("""
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
    val q = Datomic.typedQuery[Args2, Args1]("""
      [
       :find ?n :in $ % 
       :where [?c :community/name ?n]
              (northern ?c)
              (social-media ?c)
      ]
    """)

    val results = Datomic.query(q, database, rule)
    
    Ok(Json.toJson(
      results.map{
        case (cname: DString) => Json.obj("community" -> cname.as[String])
        case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  def findAllDBTx = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?when :where [?tx :db/txInstant ?when]]
    """)

    val results = Datomic.query(q)

    val sorted = results.map( dd => dd.asInstanceOf[DInstant]).sortWith( (_1: DInstant, _2: DInstant) => _1.as[java.util.Date].after(_2.as[java.util.Date]) )
    val data_tx_date = sorted(0)
    val schema_tx_date = sorted(1)

    val q2 = Datomic.typedQuery[Args1, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)
    
    val nbSch = Datomic.query(q2, database.asOf(schema_tx_date)).size
    val nbData = Datomic.query(q2, database.asOf(data_tx_date)).size

    val dataContent = Source.fromInputStream(current.resourceAsStream("seattle-data1.dtm").get).mkString
    val data = Datomic.parseOps(dataContent)

    data.map { data =>
      val dbAfter = Datomic.withData(data).dbAfter
      val nbWithAfter = Datomic.query(q2, dbAfter).size
      val nbCurrent = Datomic.query(q2, database).size
      Async{
        Datomic.transact(data).map{ tx =>
          val nbTransactAfter = Datomic.query(q2, database).size
          val nbSince = Datomic.query(q2, database.since(data_tx_date)).size

          Ok(Json.toJson(
            Json.obj(
              "data" -> data_tx_date.as[Long], 
              "schema" -> schema_tx_date.as[Long],
              "nb_schema" -> nbSch,
              "nb_data" -> nbData,
              "nb_after_with" -> nbWithAfter,
              "nb_current" -> nbCurrent,
              "nb_transact_after" -> nbTransactAfter,
              "nb_since" -> nbSince
            )
          ))            
        }
      }
    }.getOrElse(BadRequest("Data not valid"))

  }

  def makeNewPartition = Action {
    val newPartition = Datomic.addToEntity(DId(Partition.DB))(
      Namespace.DB / "ident" -> ":communities",
      Namespace.DB.INSTALL / "_partition" -> "db.part/db"
    )

    Async {
      Datomic.transact(newPartition).map{ tx =>
        Ok(Json.obj("tx" -> tx.toString))
      }
    }
  }

  def makeNewComm = Action {
    val newComm = Datomic.addToEntity(DId(Partition(Keyword("communities"))))(
      Namespace("community") / "name" -> "Easton"
    )

    Async {
      Datomic.transact(newComm).map{ tx =>
        Ok(Json.obj("tx" -> tx.toString))
      }
    }
  }  

  def updateComm = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?id :where [?id :community/name "belltown"]]
    """)

    val id = Datomic.query(q).head.asInstanceOf[DLong]
    
    val op = Datomic.add(DId(id))( Namespace("community") / "category" -> "free stuff" )
    Async {
      Datomic.transact(op).map{ tx =>
        Ok(Json.obj("tx" -> tx.toString))
      }
    }

  }

  def retractComm = Action {
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?id :where [?id :community/name "belltown"]]
    """)

    val id = Datomic.query(q).head.asInstanceOf[DLong]
    
    val op = Datomic.retractEntity(id)
    Async {
      Datomic.transact(op).map{ tx =>
        Ok(Json.obj("tx" -> tx.toString))
      }
    }

  }  
  
  def transReport = Action {
    val newComm = Datomic.addToEntity(DId(Partition(Keyword("communities"))))(
      Namespace("community") / "name" -> "Easton"
    )

    //Await.result(Datomic.transact(newComm), Duration("2 seconds"))
    val txReportQueue = conn.txReportQueue

    Async{
      Datomic.transact(newComm).map{ tx =>
        play.Logger.info("hello")
        //val report = conn.connection.txReportQueue.poll()
        txReportQueue.stream.head match {
          case None => BadRequest("Unexpected result")
          case Some(report) =>
            play.Logger.info("hello2")

            val q = Datomic.typedQuery[Args2, Args4]("""
              [
               :find ?e ?aname ?v ?added
               :in $ [[?e ?a ?v _ ?added]]
               :where [?e ?a ?v _ ?added]
                      [?a :db/ident ?aname]
              ]
            """)

            Ok(Datomic.query(q, report.dbAfter, new DSet(report.txData.toSet)).collect{
              case (e, aname, v, added) => Json.arr(e.toString, aname.toString, v.toString, added.toString)
            }.foldLeft(Json.arr())( (acc, e) => acc :+ e))
        }
        
      }
    }
  }  
}