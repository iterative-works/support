package works.iterative.tapir
package endpoints

import sttp.tapir.Codec
import sttp.tapir.CodecFormat.TextPlain
import zio.json.JsonCodec
import sttp.tapir.Schema
import sttp.tapir.Endpoint
import CustomTapir.{*, given}

type BaseEndpoint = Endpoint[Unit, Unit, Unit, Unit, Any]

trait RepositoryEndpointsModule[K: Codec[
  String,
  *,
  TextPlain
], V: JsonCodec: Schema, F: JsonCodec: Schema](
    name: String,
    base: BaseEndpoint
) extends CustomTapir:
  val load: ApiEndpoint[Unit, K, Option[V]] = base.get
    .in("view" / name / path[K]("id"))
    .out(jsonBody[Option[V]])
    .toApi

  val find: ApiEndpoint[Unit, F, List[V]] = base.post
    .in("view" / name)
    .in(jsonBody[F])
    .out(jsonBody[List[V]])
    .toApi
