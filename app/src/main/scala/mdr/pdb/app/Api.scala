package mdr.pdb.app

import mdr.pdb.api.Endpoints
import sttp.client3.*
import sttp.tapir.DecodeResult
import org.scalajs.dom
import scala.concurrent.Future

class Api(base: Option[String]) extends CustomTapir:
  private val backend = FetchBackend(
    FetchOptions(
      Some(dom.RequestCredentials.`same-origin`),
      Some(dom.RequestMode.`same-origin`)
    )
  )
  private val baseUri = base.map(b => uri"${b}")
  val alive: Unit => Future[DecodeResult[Either[Unit, String]]] =
    toClient(Endpoints.alive, baseUri, backend)
