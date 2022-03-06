package mdr.pdb
package endpoints

import fiftyforms.tapir.CustomTapir

trait Endpoints extends CustomTapir:
  given schemaForOsobniCislo: Schema[OsobniCislo] = Schema.string

  val alive: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.in("alive").out(stringBody)

object Endpoints extends Endpoints
