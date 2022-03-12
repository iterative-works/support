package mdr.pdb
package proof
package query.endpoints

import works.iterative.tapir.CustomTapir
import mdr.pdb.proof.codecs.Codecs
import works.iterative.tapir.ServerError
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

object Endpoints
    extends mdr.pdb.endpoints.Endpoints
    with CustomTapir
    with Codecs:

  val forUser: Endpoint[Unit, OsobniCislo, ServerError, Seq[Proof], Any] =
    endpoint
      .in("proof")
      .in(path[OsobniCislo]("osobniCislo"))
      .out(jsonBody[Seq[Proof]])
      .errorOut(jsonBody[ServerError])
