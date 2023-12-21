package works.iterative
package autocomplete
package codecs

import zio.json.*
import works.iterative.tapir.CustomTapir.*

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs extends works.iterative.tapir.codecs.JsonCodecs:
    given JsonCodec[AutocompleteEntry] = DeriveJsonCodec.gen[AutocompleteEntry]

trait TapirCodecs extends works.iterative.tapir.codecs.TapirCodecs:
    given Schema[AutocompleteEntry] = Schema.derived[AutocompleteEntry]

object Codecs extends Codecs
