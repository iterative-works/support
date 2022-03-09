package mdr.pdb
package endpoints

import fiftyforms.tapir.CustomTapir
import sttp.tapir.Codec.PlainCodec
import mdr.pdb.codecs.TapirCodecs

trait Endpoints extends CustomTapir with TapirCodecs:
  val alive: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.in("alive").out(stringBody)

object Endpoints extends Endpoints
