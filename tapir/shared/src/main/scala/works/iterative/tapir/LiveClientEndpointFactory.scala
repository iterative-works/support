package works.iterative.tapir

import zio.*
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe

class LiveClientEndpointFactory(using
    baseUri: BaseUri,
    backend: CustomTapir.Backend
) extends ClientEndpointFactory
    with CustomTapir:

  override def umake[I, O](
      endpoint: PublicEndpoint[I, Unit, O, Any]
  ): Client[I, Nothing, O] = Client((input: I) =>
    mkClient(endpoint)(input).orDieWith(_ =>
      new IllegalStateException("Infallible endpoint failed")
    )
  )

  override def make[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  ): Client[I, E, O] = mkClient(endpoint)

  private def mkClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using
      baseUri: BaseUri,
      backend: Backend,
      wsToPipe: WebSocketToPipe[Any]
  ): Client[I, E, O] = Client((input: I) =>
    val req = toRequest(endpoint, baseUri.toUri)
    val fetch = req(input).followRedirects(false).send(backend)
    for
      resp <- fetch.orDie
      body <- resp.body match
        case DecodeResult.Value(v) => ZIO.succeed(v)
        case err: DecodeResult.Failure =>
          ZIO.die(
            new RuntimeException(
              s"Unexpected response status: ${resp.code} ${resp.statusText} - ${err}"
            )
          )
      v <- ZIO.fromEither(body)
    yield v
  )

object LiveClientEndpointFactory:
  val layer: URLayer[BaseUri & CustomTapir.Backend, ClientEndpointFactory] =
    ZLayer {
      for
        given BaseUri <- ZIO.service[BaseUri]
        given CustomTapir.Backend <- ZIO.service[CustomTapir.Backend]
      yield LiveClientEndpointFactory()
    }

  val default: ZLayer[BaseUri, Throwable, ClientEndpointFactory] =
    CustomTapir.clientLayer >>> layer
