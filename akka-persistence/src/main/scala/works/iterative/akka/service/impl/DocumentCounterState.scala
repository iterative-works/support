package works.iterative
package akka
package service.impl

import zio.json.*
import zio.json.ast.Json
import scala.util.Try

// TODO: could use the one in core codecs, but this does not depends on core codecs
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

case class DocumentCounterState(counter: Int)

object DocumentCounterState:
    given JsonCodec[DocumentCounterState] =
        DeriveJsonCodec.gen[DocumentCounterState]

enum DocumentCounterEvent:
    case Incremented

given JsonCodec[DocumentCounterEvent] =
    val derived = DeriveJsonCodec.gen[DocumentCounterEvent]
    val legacy = legacyEnumDecoder[DocumentCounterEvent](
        DocumentCounterEvent.values,
        DocumentCounterEvent.valueOf
    )
    JsonCodec(derived.encoder, legacy.orElse(derived.decoder))
end given

class DocumentCounterEventSerializer
    extends AkkaZioJsonSerializer[DocumentCounterEvent](
        identifier = 1241001,
        manifestDiscriminator = _.getClass.getSimpleName
    )

class DocumentCounterStateSerializer
    extends AkkaZioJsonSerializer[DocumentCounterState](
        identifier = 1241002,
        manifestDiscriminator = _.getClass.getSimpleName
    )
