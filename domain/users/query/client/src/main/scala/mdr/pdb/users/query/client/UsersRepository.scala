package mdr.pdb.users.query
package client

import endpoints.Endpoints

import zio.*
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.BaseUri

trait UsersRepository:
  def list(): Task[List[UserInfo]]

object UsersRepository:
  def list(): RIO[UsersRepository, List[UserInfo]] =
    ZIO.serviceWithZIO(_.list())

object UsersRepositoryLive:

  val layer: URLayer[BaseUri & CustomTapir.Backend, UsersRepository] =
    (UsersRepositoryLive(using _, _)).toLayer

class UsersRepositoryLive(using baseUri: BaseUri, backend: CustomTapir.Backend)
    extends UsersRepository
    with CustomTapir:
  private val listClient = makeClient(Endpoints.list)
  override def list(): Task[List[UserInfo]] =
    ZIO.fromFuture(_ => listClient(()))
