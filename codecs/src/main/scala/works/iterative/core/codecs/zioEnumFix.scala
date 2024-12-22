package works.iterative.core.codecs

import zio.json.*
import zio.json.ast.Json
import scala.util.Try

// TODO: make generic
def legacyEnumDecoder[T <: scala.reflect.Enum](
    elems: Array[T],
    construct: String => T
): JsonDecoder[T] =
    JsonDecoder[Json.Obj].mapOrFail: obj =>
        val values = elems.map(_.toString())
        obj.fields.headOption match
            case Some((k, _)) if values.contains(k) =>
                Try(construct(k)).toEither.left.map(_.getMessage())
            case _ => Left("Invalid value")
