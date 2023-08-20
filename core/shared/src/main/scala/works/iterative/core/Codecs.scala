package works.iterative.core

import zio.json.*

trait Codecs:
  given JsonCodec[Email] =
    JsonCodec.string.transformOrFail(
      Email(_).toEitherWith(_ => "Error parsing email"),
      _.value
    )
  given JsonCodec[PlainMultiLine] = JsonCodec.string.transformOrFail(
    PlainMultiLine(_).toEitherWith(_ => "Error parsing PlainMultiLine"),
    _.asString
  )

object Codecs extends Codecs
