package mdr.pdb
package api

import sttp.tapir.Endpoint
import mdr.pdb.api.Endpoints.ServerError
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

object Endpoints extends CustomTapir:

  given schemaForOsobniCislo: Schema[OsobniCislo] = Schema.string

  sealed trait ServerError
  case class InternalServerError(msg: String) extends ServerError
  object InternalServerError:
    def fromThrowable(t: Throwable): ServerError = InternalServerError(
      t.getMessage
    )

  object ServerError:
    given JsonCodec[ServerError] = DeriveJsonCodec.gen

  val alive: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.in("alive").out(stringBody)

  val users: Endpoint[Unit, Unit, ServerError, List[UserInfo], Any] =
    endpoint
      .in("users")
      .out(jsonBody[List[UserInfo]])
      .errorOut(jsonBody[ServerError])
