package mdr.pdb.proof.command
package client

import endpoints.Endpoints

import zio.*
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.BaseUri

trait ProofCommandApi:
  def submitCommand(command: Command): Task[Unit]

object ProofCommandApi:
  def submitCommand(command: Command): RIO[ProofCommandApi, Unit] =
    ZIO.serviceWithZIO(_.submitCommand(command))

object ProofCommandApiLive:
  val layer: URLayer[BaseUri & CustomTapir.Backend, ProofCommandApi] =
    (ProofCommandApiLive(using _, _)).toLayer

class ProofCommandApiLive(using baseUri: BaseUri, backend: CustomTapir.Backend)
    extends ProofCommandApi
    with CustomTapir:
  private val submitCommandClient = makeClient(Endpoints.submitCommand)
  override def submitCommand(command: Command): Task[Unit] =
    ZIO.fromFuture(_ => submitCommandClient(command))
