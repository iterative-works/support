package works.iterative.tapir

import zio.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import scala.concurrent.Future
import sttp.client3.SttpBackend
import sttp.capabilities.WebSockets
import scala.concurrent.ExecutionContext
import sttp.client3.FetchBackend
import sttp.client3.FetchOptions
import org.scalajs.dom

trait CustomTapirPlatformSpecific extends SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Future, WebSockets]

  val clientLayer: ULayer[Backend] = ZLayer.succeed(
    FetchBackend(
      FetchOptions(
        Some(dom.RequestCredentials.`same-origin`),
        Some(dom.RequestMode.`same-origin`)
      )
    )
  )

  def makeClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using
      baseUri: BaseUri,
      backend: Backend,
      wsToPipe: WebSocketToPipe[Any]
  ): I => Future[O] =
    val req = toRequestThrowErrors(endpoint, baseUri.toUri)
    (i: I) => {
      val resp = backend.responseMonad.map(
        backend.send(req(i).followRedirects(false))
      )(_.body)
      resp.onComplete {
        case scala.util.Failure(e: RuntimeException)
            if e.getMessage == "Unexpected redirect" =>
          // Reload window on redirect, as it means that we need to log in again
          org.scalajs.dom.window.location.reload(true)
        case _ => ()
      }(using ExecutionContext.global)
      resp
    }
