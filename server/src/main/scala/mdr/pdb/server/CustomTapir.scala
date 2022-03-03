package mdr.pdb.server

import sttp.tapir.Tapir
import sttp.tapir.TapirAliases
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio.*
import sttp.tapir.ztapir.ZTapir

object CustomTapir
    extends Tapir
    with ZTapir
    with ZHttp4sServerInterpreter[AppEnv]
    with TapirJsonZio
    with TapirAliases
