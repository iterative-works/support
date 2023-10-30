package works.iterative
package core
package codecs

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.event.EventRecord

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs extends works.iterative.tapir.codecs.JsonCodecs:
  given JsonCodec[EventRecord] = DeriveJsonCodec.gen[EventRecord]

trait TapirCodecs extends works.iterative.tapir.codecs.TapirCodecs:
  given Schema[EventRecord] = Schema.derived[EventRecord]

object Codecs extends Codecs
