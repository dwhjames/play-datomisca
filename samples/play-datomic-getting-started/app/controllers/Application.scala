package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

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

    val eid = Datomic.query(q).head.as[DLong]
    
    eid.map{ eid => 
      database.entity(eid).map{ e =>
        Ok(Json.toJson(e.toMap.map{ case(k, v) => k.toString -> v.toString }))
      }.getOrElse(BadRequest("Entity not found"))
    }.recover{
      case e: Exception => BadRequest(e.getMessage)
    }.get
    
  }

  def getCommunityNames = Action{
    val q = Datomic.typedQuery[Args0, Args1]("""
      [:find ?c :where [?c :community/name]]
    """)

    val l = Datomic.query(q).collect{
      case eid: DLong => 
        database.entity(eid).map{ entity =>
          entity.as[DString](community / "name").map(Datomic.fromDatomic[String](_))
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
          entity.as[DEntity](community / "neighborhood").map(_.toMap.map{ case(k, v) => k.toString -> v.toString })
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

    Datomic.query(q).head.as[DLong].flatMap { eid =>
      database.entity(eid).map{ entity =>
        entity.as[DEntity](community / "neighborhood").flatMap { neighborhood => 
          neighborhood.as[DSet](community / "_neighborhood").flatMap { communities =>
            val l = communities.toSet.map { comm =>
              comm.as[DEntity].flatMap{ ent => ent.as[DString](community / "name") }.map(_.value)
            }
            Utils.sequence(l)
          }
        }.map{ l =>
          Ok(Json.toJson(l))
        }
      }.get
    }.recover{
      case e: Exception => BadRequest(e.getMessage)
    }.get
    

    
  }

  def findAllCommunityNames = Action {
    val q=  Datomic.typedQuery[Args0, Args2]("""
      [:find ?c ?n :where [?c :community/name ?n]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).collect{
        case (eid: DLong, name: DString) => name.value
      }
    ))
  }

  def findAllCommunityNamesAndUrls = Action {
    val q = Datomic.typedQuery[Args0, Args2]("""
      [:find ?n ?u :where [?c :community/name ?n][?c :community/url ?u]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).collect{
        case (name: DString, url: DString) => Json.obj("name" -> name.value, "url" -> url.value)
      }
    ))
  }

  def findCategoriesForBelltown = Action {
    val q = Datomic.typedQuery[Args0, Args2]("""
      [:find ?e ?c :where [?e :community/name "belltown"][?e :community/category ?c]]
    """)

    Ok(Json.toJson(
      Datomic.query(q).map{
        case (e: DLong, c: DString) => Json.obj("id" -> e.value, "url" -> c.value)
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
        case (e: DString) => e.value
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
        case (e: DString) => e.value
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
        case (cname: DString, rname: DRef) => Json.obj(cname.value -> rname.toString)
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
        case (name: DString) => name.value
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
          case (cname: DString, tpe: DString) => Json.obj(cname.value -> tpe.value)
          case t => throw new RuntimeException("unexpected type:"+t.getClass)
      }
    ))
  }

  /*def findNonCommercialEmaillistOrCommercialWebSites = Action {
    Datomic.query[Args2, Args2]("""
      [
       :find ?n ?t ?ot 
       :in $ [[?t ?ot]] 
       :where [?c :community/name ?n]
              [?c :community/type ?t]
              [?c :community/orgtype ?ot]
      ]
    """).all()
        .execute(
          database, 
          DSet(
            DSet(DString(":community.type/email-list"), DString(":community.orgtype/community")),
            DSet(DString(":community.type/website"), DString(":community.orgtype/commercial"))
          )
        ).map{ results =>
          Ok(Json.toJson(
            results.map{
              case (cname: DString, tpe: DString) => Json.obj(cname.value -> tpe.value)
              case t => throw new RuntimeException("unexpected type:"+t.getClass)
            }
          ))
        }.recover{
          case e: Exception => BadRequest(e.getMessage)
        }.get
  }*/

}