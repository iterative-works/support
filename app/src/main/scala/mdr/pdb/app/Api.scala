package mdr.pdb
package app

import zio.*
import mdr.pdb.api.Endpoints
import sttp.client3.*
import sttp.tapir.DecodeResult
import org.scalajs.dom
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe

trait Api:
  def isAlive(): Task[Boolean]
  def listUsers(): Task[List[UserInfo]]

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

  private def makeClient[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  )(using wsToPipe: WebSocketToPipe[Any]): I => Future[O] =
    toClientThrowErrors(endpoint, baseUri, backend)

  private val aliveClient = makeClient(Endpoints.alive)
  override def isAlive(): Task[Boolean] =
    ZIO.fromFuture(_ => aliveClient(())).fold(_ => false, _ => true)

  private val usersClient = makeClient(Endpoints.users)
  override def listUsers(): Task[List[UserInfo]] =
    ZIO.fromFuture(_ => usersClient(()))
