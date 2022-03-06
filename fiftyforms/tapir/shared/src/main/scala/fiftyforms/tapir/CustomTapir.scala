package fiftyforms.tapir

import sttp.tapir.Tapir
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.TapirAliases
import sttp.tapir.generic.SchemaDerivation

trait CustomTapir
    extends Tapir
    with TapirJsonZio
    with TapirAliases
    with SchemaDerivation
    with CustomTapirPlatformSpecific

object CustomTapir extends CustomTapir
