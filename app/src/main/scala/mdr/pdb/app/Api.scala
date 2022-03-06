package mdr.pdb
package app

import zio.*
import sttp.client3.*
import sttp.tapir.DecodeResult
import org.scalajs.dom
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.WebSocketToPipe
import fiftyforms.tapir.{CustomTapir, BaseUri}
import endpoints.Endpoints

trait Api:
  def alive(): Task[Boolean]

object ApiLive:
  val layer: URLayer[BaseUri & CustomTapir.Backend, Api] =
    (ApiLive(using _, _)).toLayer

class ApiLive(using baseUri: BaseUri, backend: CustomTapir.Backend)
    extends Api
    with CustomTapir:
  private val aliveClient = makeClient(Endpoints.alive)
  override def alive(): Task[Boolean] =
    ZIO.fromFuture(_ => aliveClient(())).fold(_ => false, _ => true)
