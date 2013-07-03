package play.modules.datomisca

import datomisca._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.libs.functional.{Monoid, Reducer}

object Implicits {

  implicit lazy val DD2Json: Writes[DatomicData] = Writes[DatomicData] {
    case DString(s)   => JsString(s)
    case DBoolean(b)  => JsBoolean(b)
    case DLong(l)     => JsNumber(l)
    case DFloat(f)    => JsNumber(f)
    case DDouble(d)   => JsNumber(d)
    case DBigInt(bi)  => JsNumber(BigDecimal(bi).bigDecimal)
    case DBigDec(bd)  => JsNumber(bd)
    case DInstant(di) => JsNumber(di.getTime)
    case DUuid(uuid)  => JsString(uuid.toString)
    case DUri(uri)    => JsString(uri.toString)
    case DKeyword(kw) => JsString(kw.toString)
    case e: DEntity   => Json.toJson(e.toMap map { case (k, v) => k -> Json.toJson(v)(DD2Json) })
    case coll: DColl  => Json.toJson(coll.toIterable.map(v => Json.toJson(v)(DD2Json)))
    case DRef.IsKeyword(kw)     => JsString(kw.toString)
    case DRef.IsId(id: FinalId) => JsNumber(id.underlying)
    case d => throw new RuntimeException("unable to convert this DatomicData type to Json: "+d.getClass)
  }

  implicit val readsDRef = Reads[DRef] {
    case JsString(kw) => JsSuccess(DRef(Keyword(kw)))
    case JsNumber(id) => JsSuccess(DRef(DId(id.toLong)))
    case _ => JsError("datomic.expected.jsstringorjsnumber")
  }

  implicit val Keyword2Json = Writes[Keyword] { kw => JsString(kw.toString) }
  implicit val DEntity2Json = Writes[DEntity] { e => Json.toJson(e.toMap) }

  implicit val PartialAddToEntityMonoid: Monoid[PartialAddEntity] = new Monoid[PartialAddEntity] {
    def append(a1: PartialAddEntity, a2: PartialAddEntity) = a1 ++ a2
    def identity = PartialAddEntity.empty
  }

  implicit val PartialAddToEntityReducer: Reducer[PartialAddEntity, PartialAddEntity] = Reducer( p => p )

  def readAttr[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) =
      Reads[PartialAddEntity] { js =>
        js.validate[T](jsReads) map { t =>
          ac.convert(attr).write(t)
        }
      }
  }

  def readAttrWithNs[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) =
      Reads[PartialAddEntity] { js =>
        js.validate[T](jsReads) map { t =>
          ac.convert(attr).write(t)
        }
      }
  }

  def writeAttr[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2EntityReaderCast[DD, Card, T], jsWrites: Writes[T]) =
      Writes[DEntity] { entity =>
        jsWrites.writes(ac.convert(attr).read(entity))
      }
  } 

  def writeAttrWithNs[T] = new {
    def apply[DD <: DatomicData, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2EntityReaderCast[DD, Card, T], jsWrites: Writes[T]) =
      Writes[DEntity] { entity =>
        jsWrites.writes(ac.convert(attr).read(entity))
      }
  }

}
