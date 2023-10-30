package works.iterative.tapir

import sttp.tapir.Tapir
import sttp.tapir.json.zio.TapirJsonZio
import sttp.tapir.TapirAliases
import works.iterative.core.auth.AccessToken
import sttp.capabilities.zio.ZioStreams
import works.iterative.core.auth.service.AuthenticationError
import sttp.model.StatusCode
import works.iterative.tapir.codecs.Codecs.given
import zio.*
import zio.json.*

trait CustomTapir
    extends Tapir
    with TapirJsonZio
    with TapirAliases
    with CustomTapirPlatformSpecific

object CustomTapir extends CustomTapir:
  type ApiError[+E] = works.iterative.tapir.ApiError[E]
  val ApiError = works.iterative.tapir.ApiError

  type ApiEndpoint[E, I, O] = works.iterative.tapir.ApiEndpoint[E, I, O]

  given apiRequestFailureCodec[E: JsonCodec]
      : JsonCodec[ApiError.RequestFailure[E]] =
    DeriveJsonCodec.gen
  given apiRequestFailureSchema[E: Schema]: Schema[ApiError.RequestFailure[E]] =
    Schema.derived

  given authenticationErrorCodec: JsonCodec[AuthenticationError] =
    DeriveJsonCodec.gen
  given authFailureCodec: JsonCodec[ApiError.AuthFailure] = DeriveJsonCodec.gen
  given authenticationErrorSchema: Schema[AuthenticationError] = Schema.derived
  given authFailureSchema: Schema[ApiError.AuthFailure] = Schema.derived

  given JsonCodec[Unit] = JsonCodec.string.transform(_ => (), _ => "")

  extension [I, O](base: Endpoint[Unit, I, Unit, O, ZioStreams])
    def toApi[E: JsonCodec: Schema]: ApiEndpoint[E, I, O] =
      base
        .securityIn(auth.bearer[AccessToken]())
        .errorOut(
          oneOf[ApiError[E]](
            oneOfVariant[ApiError.AuthFailure](
              StatusCode.Unauthorized,
              jsonBody[ApiError.AuthFailure]
            ),
            oneOfDefaultVariant[ApiError.RequestFailure[E]](
              statusCode(StatusCode.BadRequest)
                .and(jsonBody[ApiError.RequestFailure[E]])
            )
          )
        )
