package fiftyforms.tapir

import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import scala.concurrent.Future
import sttp.client3.SttpBackend
import sttp.capabilities.WebSockets

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
    toClientThrowErrors(endpoint, baseUri.toUri, backend)
