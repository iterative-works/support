package works.iterative.entity

import zio.*
import works.iterative.core.UserMessage
import works.iterative.core.service.IdGenerator

sealed trait FactoryError:
  def entityId: String
  def userMessage: UserMessage

object FactoryError:
  case class EntityAlreadyExists[Id](entityId: String, id: Id)
      extends FactoryError:
    def userMessage =
      UserMessage(s"${entityId}.error.entity.exists", id.toString())

  case class EntityNotFound[Id](entityId: String, id: Id) extends FactoryError:
    def userMessage =
      UserMessage(s"${entityId}.error.entity.not.found", id.toString())

trait AggregateRootFactory[
    Ident,
    T <: AggregateRoot[Ident, ?, ?, ?, ?]
]:
  type Id = Ident
  type NotFound = FactoryError.EntityNotFound[Id]
  type AlreadyExists = FactoryError.EntityAlreadyExists[Id]

  protected def AlreadyExists(id: Id): AlreadyExists =
    FactoryError.EntityAlreadyExists(entityId, id)

  protected def NotFound(id: Id): NotFound =
    FactoryError.EntityNotFound(entityId, id)

  def entityId: String
  def make(id: Id): IO[AlreadyExists, T]
  def load(id: Id): IO[NotFound, T]

  def loadOrMake(id: Id): UIO[T] =
    load(id)
      .orElse(make(id))
      .orDieWith(_ =>
        new RuntimeException(
          s"Loading or making entity ${entityId} with id ${id} failed, loading fails with NotFound, making fails with AlreadyExists"
        )
      )

  def make()(using idGen: IdGenerator[Id]): UIO[T] =
    val tryToCreate = for
      id <- idGen.nextId
      entity <- make(id)
    yield entity

    tryToCreate
      .retryN(10)
      .orDieWith(_ =>
        new RuntimeException(
          "Cannot create entity, generator failed 10 times to create new ID"
        )
      )
