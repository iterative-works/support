package works.iterative.tapir

import zio.*
import sttp.tapir.Endpoint
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp.ws.zio.*

class LiveClientEndpointFactory(using
    baseUri: BaseUri,
    backend: CustomTapir.Backend
) extends ClientEndpointFactory
    with CustomTapir:

  override def makeSecureClient[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, ZioStreams & WebSockets],
      isWebSocket: Boolean = false
  ): S => I => IO[E, O] = (securityInput: S) => (input: I) =>
    val req = toSecureRequest(
      endpoint,
      if isWebSocket then
        baseUri.toUri.map(b =>
          b.scheme match
            case Some("https") => b.scheme("wss")
            case _             => b.scheme("ws")
        )
      else baseUri.toUri
    )

    val fetch = req(securityInput)(input).followRedirects(false).send(backend)

    val result = for
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

    result

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
