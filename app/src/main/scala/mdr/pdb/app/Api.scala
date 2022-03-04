package mdr.pdb.app

import zio.*
import mdr.pdb.api.Endpoints
import sttp.client3.*
import sttp.tapir.DecodeResult
import org.scalajs.dom
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait Api:
  def isAlive(): Task[Boolean]

object ApiLive:
  val layer: URLayer[AppConfig, Api] =
    ((conf: AppConfig) => ApiLive(Some(conf.baseUrl + "api/"))).toLayer

class ApiLive(base: Option[String]) extends Api with CustomTapir:
  private val backend = FetchBackend(
    FetchOptions(
      Some(dom.RequestCredentials.`same-origin`),
      Some(dom.RequestMode.`same-origin`)
    )
  )
  private val baseUri = base.map(b => uri"${b}")
  private val aliveClient = toClient(Endpoints.alive, baseUri, backend)
  override def isAlive(): Task[Boolean] =
    ZIO.fromFuture(ec =>
      given ExecutionContext = ec
      aliveClient(()).map {
        case DecodeResult.Value(Right("ok")) => true
        case _                               => false
      } recover { case _ =>
        false
      }
    )
