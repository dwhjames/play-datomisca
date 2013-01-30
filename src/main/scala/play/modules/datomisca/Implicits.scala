package play.modules.datomisca

import datomisca._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.libs.functional.{Monoid, Reducer}

object Implicits {

  implicit lazy val DD2Json: Writes[DatomicData] = Writes[DatomicData]( dd => dd match {
    case DString(s) => JsString(s)
    case DBoolean(b) => JsBoolean(b)
    case DLong(l) => JsNumber(l)
    case DFloat(f) => JsNumber(f)
    case DDouble(d) => JsNumber(d)
    case DBigInt(bi) => JsNumber(BigDecimal(bi).bigDecimal)
    case DBigDec(bd) => JsNumber(bd)
    case DInstant(di) => JsNumber(di.getTime)
    case DUuid(uuid) => JsString(uuid.toString)
    case DUri(uri) => JsString(uri.toString)
    case e: DEntity => Json.toJson(e.toMap map { case(k, v) => k.name -> Json.toJson(v)(DD2Json) })
    case set: DSet => Json.toJson(set.toSet.map(v => Json.toJson(v)(DD2Json)))
    case ref: DRef => ref.underlying match {
      case Left(kw) => JsString(kw.toString)
      case Right(id: FinalId) => JsNumber(id.underlying)
      case _ => throw new RuntimeException("unable to convert this DatomicData type to Json: "+ref.getClass)
    }
    case d => throw new RuntimeException("unable to convert this DatomicData type to Json: "+d.getClass)
  })

  implicit def Ref2Json[T](implicit wrt: Writes[T]): Writes[Ref[T]] = Writes[Ref[T]]{ ref =>
    wrt.writes(ref.ref)
  }

  implicit val readsDRef = Reads[DRef]( js => js match {
    case JsString(kw) => JsSuccess(DRef(Keyword(kw)))
    case JsNumber(id) => JsSuccess(DRef(DId(id.toLong)))
    case _ => JsError("datomic.expected.jsstringorjsnumber")
  })

  implicit val Keyword2Json = Writes[Keyword]( kw => JsString(kw.toString))
  implicit val DEntity2Json = Writes[DEntity]( entity => Json.toJson(entity.toMap map { case(k,v) => k.toString -> v }) )

  implicit val PartialAddToEntityMonoid: Monoid[PartialAddEntity] = new Monoid[PartialAddEntity] {
    def append(a1: PartialAddEntity, a2: PartialAddEntity) = a1 ++ a2
    def identity = PartialAddEntity.empty
  }

  implicit val PartialAddToEntityReducer: Reducer[PartialAddEntity, PartialAddEntity] = Reducer( p => p )

  /*def readAttr[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])
  (implicit ac: Attribute2PartialAddToEntityWriter[DD, Card, T], jsReads: Reads[T]) = {
    Reads[PartialAddEntity]{ js => 
      (__ \ attr.toString).asSingleJsResult(js) flatMap { jsv =>
        jsv.validate[T](jsReads) map { t =>
          ac.convert(attr).write(t)
        }
      }
    }
  }*/

  def readAttr[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
      (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) = {
      Reads[PartialAddEntity]{ js => 
          js.validate[T](jsReads) map { t =>
            ac.convert(attr).write(t)
          }
      }
    }
  }

  def readAttrWithNs[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
      (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) = {
      Reads[PartialAddEntity]{ js => 
          js.validate[T](jsReads) map { t =>
            ac.convert(attr).write(t)
          }
      }
    }
  }

  def writeAttr[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
      (implicit ac: Attribute2EntityReader[DD, Card, T], jsWrites: Writes[T]) = {
      Writes[DEntity]{ entity => 
        jsWrites.writes(ac.convert(attr).read(entity))
      }
    }
  } 

  def writeAttrWithNs[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
      (implicit ac: Attribute2EntityReader[DD, Card, T], jsWrites: Writes[T]) = {
      Writes[DEntity]{ entity => 
        jsWrites.writes(ac.convert(attr).read(entity))
      }
    }
  } 
}