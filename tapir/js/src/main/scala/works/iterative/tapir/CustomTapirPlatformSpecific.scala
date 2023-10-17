package works.iterative.tapir

import zio.*
import zio.stream.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import sttp.client3.SttpBackend
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.FetchOptions
import org.scalajs.dom
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.DecodeResult
import works.iterative.core.FileSupport
import works.iterative.core.FileSupport.*
import sttp.model.Part

trait CustomTapirPlatformSpecific extends SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Task, ZioStreams & WebSockets]

  val clientLayer: ULayer[Backend] = ZLayer.succeed(
    FetchZioBackend(
      FetchOptions(
        Some(dom.RequestCredentials.`same-origin`),
        Some(dom.RequestMode.`same-origin`)
      )
    )
  )

  extension (f: FileRepr)
    def toPart: Task[Part[Array[Byte]]] =
      f.toStream.run(ZSink.collectAll).map { bytes =>
        Part("file", bytes.toArray, fileName = Some(f.name))
      }

  def makeClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using
      baseUri: BaseUri,
      backend: Backend,
      wsToPipe: WebSocketToPipe[Any]
  ): I => IO[E, O] = input =>
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
/*
    response(_).map(_.body).tapError {
      // TODO: remove this handler from here, the app should decide what to do on auth failure
      // Which is what the redirect here is
      case e: RuntimeException if e.getMessage == "Unexpected redirect" =>
        // Reload window on redirect, as it means that we need to log in again
        ZIO.attempt(org.scalajs.dom.window.location.reload())
      case _ => ZIO.unit
    }
 */
