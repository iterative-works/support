package works.iterative.tapir

import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import scala.concurrent.Future
import sttp.client3.SttpBackend
import sttp.capabilities.WebSockets
import scala.concurrent.ExecutionContext

trait CustomTapirPlatformSpecific extends SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Future, WebSockets]

  def makeClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using
      baseUri: BaseUri,
      backend: Backend,
      wsToPipe: WebSocketToPipe[Any]
  ): I => Future[O] =
    val req = toRequestThrowErrors(endpoint, baseUri.toUri)
    (i: I) => {
      val resp = backend.responseMonad.map(backend.send(req(i).followRedirects(false)))(_.body)
      resp.onComplete {
        case scala.util.Failure(e: RuntimeException) if e.getMessage == "Unexpected redirect" =>
          // Reload window on redirect, as it means that we need to log in again
          org.scalajs.dom.window.location.reload(true)
        case _ => ()
      }(using ExecutionContext.global)
      resp
    }
