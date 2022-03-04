package mdr.pdb
package api

object Endpoints extends CustomTapir:

  given schemaForOsobniCislo: Schema[OsobniCislo] = Schema.string

  val alive: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.in("alive").out(stringBody)

  val users =
    endpoint.in("users").out(jsonBody[List[UserInfo]])
