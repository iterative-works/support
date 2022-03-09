package mdr.pdb
package codecs

import zio.json.*
import fiftyforms.tapir.CustomTapir

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs:
  given JsonCodec[WhoWhen] = DeriveJsonCodec.gen
  given JsonCodec[OsobniCislo] =
    JsonCodec.string.transform(OsobniCislo.apply, _.toString)
  given JsonFieldEncoder[OsobniCislo] =
    JsonFieldEncoder.string.contramap(_.toString)
  given JsonFieldDecoder[OsobniCislo] =
    JsonFieldDecoder.string.map(OsobniCislo(_))

trait TapirCodecs extends CustomTapir:
  given Schema[OsobniCislo] = Schema.string
  given Codec.PlainCodec[OsobniCislo] =
    Codec.string.mapDecode(OsobniCislo.apply andThen DecodeResult.Value.apply)(
      _.toString
    )
  given Schema[WhoWhen] = Schema.derived

object Codecs extends Codecs
