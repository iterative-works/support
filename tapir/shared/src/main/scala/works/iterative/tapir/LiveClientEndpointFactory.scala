package works.iterative.tapir

import zio.*
import sttp.tapir.PublicEndpoint
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp.ws.zio.*

class LiveClientEndpointFactory(using
    baseUri: BaseUri,
    backend: CustomTapir.Backend
) extends ClientEndpointFactory
    with CustomTapir:

  override def umake[I, O](
      endpoint: PublicEndpoint[I, Unit, O, Any]
  ): Client[I, Nothing, O] = Client((input: I) =>
    mkClient(endpoint)(input).orDieWith(_ =>
      new IllegalStateException("Internal Server Error")
    )
  )

  override def make[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  ): Client[I, E, O] = mkClient(endpoint)

  override def stream[I, E, O](
      endpoint: PublicEndpoint[
        Unit,
        E,
        ZioStreams.Pipe[I, O],
        ZioStreams & WebSockets
      ]
  ): Client[Unit, E, ZioStreams.Pipe[I, O]] =
    mkClient(endpoint, true)

  override def ustream[I, O](
      endpoint: PublicEndpoint[
        Unit,
        Unit,
        ZioStreams.Pipe[I, O],
        ZioStreams & WebSockets
      ]
  ): Client[Unit, Nothing, ZioStreams.Pipe[I, O]] = Client(
    mkClient(endpoint, true)(_).orDieWith(_ =>
      new IllegalStateException("Internal Server Error")
    )
  )

  private def mkClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, ZioStreams & WebSockets],
      isWebSocket: Boolean = false
  )(using
      baseUri: BaseUri,
      backend: Backend
  ): Client[I, E, O] = Client((input: I) =>
    val req = toRequest(
      endpoint,
      if isWebSocket then
        baseUri.toUri.map(b =>
          b.scheme match
            case Some("https") => b.scheme("wss")
            case _             => b.scheme("ws")
        )
      else baseUri.toUri
    )
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
