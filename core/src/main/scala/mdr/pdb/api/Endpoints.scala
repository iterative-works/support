package mdr.pdb.api

import sttp.tapir.Endpoint

object Endpoints extends CustomTapir:

  val alive: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.in("alive").out(stringBody)
