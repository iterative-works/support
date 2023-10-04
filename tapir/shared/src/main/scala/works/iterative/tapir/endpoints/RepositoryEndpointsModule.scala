package works.iterative.tapir
package endpoints

import sttp.tapir.Codec
import sttp.tapir.CodecFormat.TextPlain
import zio.json.JsonCodec
import sttp.tapir.Schema
import sttp.tapir.Endpoint

type BaseEndpoint = Endpoint[Unit, Unit, Unit, Unit, Any]

trait RepositoryEndpointsModule[K: Codec[
  String,
  *,
  TextPlain
], V: JsonCodec: Schema, F: JsonCodec: Schema](
    name: String,
    base: BaseEndpoint
) extends CustomTapir:
  val load: Endpoint[Unit, K, Unit, Option[V], Any] = base.get
    .in("view" / name / path[K]("id"))
    .out(jsonBody[Option[V]])

  val find: Endpoint[Unit, F, Unit, List[V], Any] = base.post
    .in("view" / name)
    .in(jsonBody[F])
    .out(jsonBody[List[V]])
