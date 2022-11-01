package works.iterative.tapir

import zio.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import scala.concurrent.Future
import sttp.client3.SttpBackend
import sttp.capabilities.WebSockets
import scala.concurrent.ExecutionContext
import sttp.client3.FetchOptions
import org.scalajs.dom
import sttp.client3.impl.zio.FetchZioBackend

trait CustomTapirPlatformSpecific extends SttpClientInterpreter:
  self: CustomTapir =>

  type Backend = SttpBackend[Task, WebSockets]

  val clientLayer: ULayer[Backend] = ZLayer.succeed(
    FetchZioBackend(
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
  ): I => Task[O] =
    val req = toRequestThrowErrors(endpoint, baseUri.toUri)
    req(_).followRedirects(false).send(backend).map(_.body).tapError {
      case e: RuntimeException if e.getMessage == "Unexpected redirect" =>
        // Reload window on redirect, as it means that we need to log in again
        ZIO.attempt(org.scalajs.dom.window.location.reload(true))
      case _ => ZIO.unit
    }
