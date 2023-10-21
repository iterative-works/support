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

  def makeRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, ZioStreams & WebSockets]
  )(using ext: BaseUriExtractor[O]) = toSecureRequest(
    endpoint,
    ext.extractBaseUri
  )

  override def make[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, ZioStreams & WebSockets]
  )(using
      ext: BaseUriExtractor[O],
      em: ClientErrorConstructor[E],
      m: ClientResultConstructor[S, I, em.Error, O]
  ): m.Result = m.makeResult((securityInput: S) =>
    (input: I) =>
      val req = makeRequest(endpoint)
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

      em.mapErrorCause(result)
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
