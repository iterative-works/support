package fiftyforms.tapir

import sttp.tapir.Tapir
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.TapirAliases

trait CustomTapir
    extends Tapir
    with TapirJsonZio
    with TapirAliases
    with CustomTapirPlatformSpecific:
  given Schema[ServerError] = Schema.derived

object CustomTapir extends CustomTapir
