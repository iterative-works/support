package works.iterative.tapir

import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter

trait Http4sCustomTapir[Env]
    extends CustomTapir
    with ZHttp4sServerInterpreter[Env]
