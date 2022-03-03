package mdr.pdb.app

import zio.*
import mdr.pdb.api.Endpoints
import sttp.client3.*
import sttp.tapir.DecodeResult
import org.scalajs.dom
import scala.concurrent.Future

trait Api:
  def alive(): Future[DecodeResult[Either[Unit, String]]]

object ApiLive:
  def layer(base: Option[String]): ULayer[Api] = ZLayer.succeed(ApiLive(base))

class ApiLive(base: Option[String]) extends Api with CustomTapir:
  private val backend = FetchBackend(
    FetchOptions(
      Some(dom.RequestCredentials.`same-origin`),
      Some(dom.RequestMode.`same-origin`)
    )
  )
  private val baseUri = base.map(b => uri"${b}")
  private val aliveClient = toClient(Endpoints.alive, baseUri, backend)
  override def alive(): Future[DecodeResult[Either[Unit, String]]] =
    aliveClient(())
