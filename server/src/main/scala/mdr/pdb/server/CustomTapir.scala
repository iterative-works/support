package mdr.pdb.server

import sttp.tapir.Tapir
import sttp.tapir.TapirAliases
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio.*
import sttp.tapir.ztapir.ZTapir

trait CustomTapir
    extends mdr.pdb.api.CustomTapir
    with ZHttp4sServerInterpreter[AppEnv]

object CustomTapir extends CustomTapir
