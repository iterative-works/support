package works.iterative.entity

import zio.*
import works.iterative.core.auth.CurrentUser

trait EntityUpdateService[Id, Command, Error <: AggregateError]:
  type Op[A] = ZIO[CurrentUser, Error, A]

  def update(id: Id, command: Command): Op[Unit]

trait EntityCreateService[Id, Init, Error <: AggregateError]:
  type Op[A] = ZIO[CurrentUser, Error, A]

  def create(initData: Init): Op[Id]

trait EntityService[Id, Command, Error <: AggregateError, Init <: Command]
    extends EntityCreateService[Id, Init, Error]
    with EntityUpdateService[Id, Command, Error]
