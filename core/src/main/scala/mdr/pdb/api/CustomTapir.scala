package mdr.pdb.api

import sttp.tapir.Tapir
import sttp.tapir.ztapir.ZTapir
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.TapirAliases

trait CustomTapir extends Tapir with ZTapir with TapirJsonZio with TapirAliases

object CustomTapir extends CustomTapir
