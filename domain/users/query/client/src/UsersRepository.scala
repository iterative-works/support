package mdr.pdb.users.query
package client

import endpoints.Endpoints

import zio.*
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.BaseUri

trait UsersRepository:
  def matching(criteria: Criteria): Task[List[UserInfo]]

object UsersRepository:
  def matching(criteria: Criteria): RIO[UsersRepository, List[UserInfo]] =
    ZIO.serviceWithZIO(_.matching(criteria))

object UsersRepositoryLive:

  val layer: URLayer[BaseUri & CustomTapir.Backend, UsersRepository] =
    (UsersRepositoryLive(using _, _)).toLayer

class UsersRepositoryLive(using baseUri: BaseUri, backend: CustomTapir.Backend)
    extends UsersRepository
    with CustomTapir:
  private val matchingClient = makeClient(Endpoints.matching)
  override def matching(criteria: Criteria): Task[List[UserInfo]] =
    ZIO.fromFuture(_ => matchingClient(criteria))
