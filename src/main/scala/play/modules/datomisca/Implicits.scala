package play.modules.datomisca

import datomisca._

import play.api.libs.json._
import play.api.libs.functional.{Monoid, Reducer}
import play.api.data.validation.ValidationError

object Implicits {

  implicit val formatKeyword = Format[Keyword](
      Reads[Keyword]{
        case JsString(kwStr) =>
          if (kwStr.length < 2)
            JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.nonempty"))))
          else if (kwStr.charAt(0) != ':')
            JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.clojurekeyword"))))
          else
            JsSuccess(clojure.lang.Keyword.intern(kwStr.substring(1)))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      },
      Writes[Keyword](kw => JsString(kw.toString))
    )

  def writesDatomicDataToDepth(depth: Int): Writes[Any] = {
    require(depth >= 0)
    new Writes[Any] {
      override def writes(a: Any): JsValue = a match {
        case s: String         => JsString(s)
        case b: Boolean        => JsBoolean(b)
        case l: Long           => JsNumber(l)
        case f: Float          => JsNumber(f)
        case d: Double         => JsNumber(d)
        case bi: BigInt        => JsNumber(BigDecimal(bi))
        case bd: BigDecimal    => JsNumber(bd)
        case d: java.util.Date => Writes.DefaultDateWrites.writes(d)
        case u: java.util.UUID => JsString(u.toString)
        case u: java.net.URI   => JsString(u.toString)
        case k: Keyword        => JsString(k.toString)
        case e: Entity         =>
          if (depth == 0)
            JsNumber(e.id)
          else
            writesEntityToDepth(depth).writes(e)
        case i: Iterable[_]    =>
          val builder = Seq.newBuilder[JsValue]
          for (a <- i) {
            builder += writesDatomicDataToDepth(depth).writes(a)
          }
          JsArray(builder.result())
        case _ => throw new RuntimeException(s"Unexpected Datomic data of ${a.getClass}")
      }
    }
  }

  def writesEntityToDepth(depth: Int): Writes[Entity] = {
    require(depth > 0)
    new Writes[Entity] {
      override def writes(entity: Entity): JsValue = {
        Writes.mapWrites(writesDatomicDataToDepth(depth - 1)).writes(entity.toMap)
      }
    }
  }

  implicit val Entity2Json  = new Writes[Entity] {
    override def writes(entity: Entity): JsValue = {
      Writes.mapWrites(writesDatomicDataToDepth(0)).writes(entity.toMap)
    }
  }

  implicit val PartialAddEntityMonoid: Monoid[PartialAddEntity] = new Monoid[PartialAddEntity] {
    def append(a1: PartialAddEntity, a2: PartialAddEntity) = a1 ++ a2
    def identity = PartialAddEntity.empty
  }

  implicit val PartialAddEntityReducer: Reducer[PartialAddEntity, PartialAddEntity] = Reducer( p => p )

  @deprecated("use readFact or readNullableFact", "0.7")
  def readAttr[T] = new ReadAttrHelper[T]

  class ReadAttrHelper[T] {
    def apply[DD <: AnyRef, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) =
      Reads[PartialAddEntity] { js =>
        js.validate[T](jsReads) map { t =>
          ac.convert(attr).write(t)
        }
      }
  }

  def readFact[T] = new ReadFactHelper[T]

  class ReadFactHelper[T] {
    def apply[DD <: AnyRef, Card <: Cardinality]
             (path: JsPath, attr: Attribute[DD, Card])
             (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) =
      path.read(jsReads) map { t =>
        ac.convert(attr).write(t)
      }
  }

  def readNullableFact[T] = new ReadNullableFactHelper[T]

  class ReadNullableFactHelper[T] {
    def apply[DD <: AnyRef, Card <: Cardinality]
             (path: JsPath, attr: Attribute[DD, Card])
             (implicit ac: Attribute2PartialAddEntityWriter[DD, Card, T], jsReads: Reads[T]) =
      Reads[PartialAddEntity] { js =>
        path.asSingleJsResult(js).fold(
          error =>
            JsSuccess(PartialAddEntity.empty),
          jsval =>
            jsval.validate[T](jsReads) map { t =>
              ac.convert(attr).write(t)
            }
        )
      }
  }

  def writeAttr[T] = new WriteAttrHelper[T]

  class WriteAttrHelper[T] {
    def apply[DD <: AnyRef, Card <: Cardinality]
             (attr: Attribute[DD, Card])
             (implicit ac: Attribute2EntityReaderCast[DD, Card, T], jsWrites: Writes[T]) =
      Writes[Entity] { entity =>
        jsWrites.writes(ac.convert(attr).read(entity))
      }
  }

}
