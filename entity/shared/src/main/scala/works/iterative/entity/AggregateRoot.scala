package works.iterative.entity

import zio.*
import works.iterative.event.EventRecord
import works.iterative.core.UserMessage
import works.iterative.core.service.IdGenerator
import works.iterative.core.MessageId
import works.iterative.core.auth.CurrentUser

/** Represents aggregate root in DDD
  *
  * @tparam Id
  *   Aggregate root id
  * @tparam Command
  *   Aggregate root command
  * @tparam Event
  *   Aggregate root event
  * @tparam State
  *   Aggregate root state
  */
trait AggregateRoot[Id, Error <: AggregateError, Command, Event, State]:
    /** Aggregate root id */
    def id: Id

    /** Current state */
    def state: UIO[State]

    def handle(command: Command): IO[Error, Unit]
end AggregateRoot

trait AggregateRootModule[Id, Command, Event, State]:
    final case class ARCommand(
        id: Id,
        command: Command,
        record: EventRecord
    )

    final case class AREvent(
        id: Id,
        event: Event,
        record: EventRecord
    )

    final case class ARCommandResult(
        command: ARCommand,
        originalState: State,
        newState: State,
        events: Seq[AREvent]
    )

    sealed trait AggregateError:
        def userMessage: UserMessage

    sealed trait CommandError extends AggregateError

    case class UnhandledCommand(command: ARCommand, state: State)
        extends CommandError:
        val userMessage: UserMessage =
            UserMessage("error.unhandled.command", command.toString(), state.toString())
    end UnhandledCommand

    case class InvalidCommand(command: ARCommand, state: State, messageId: MessageId)
        extends CommandError:
        val userMessage: UserMessage =
            UserMessage(messageId, command.toString(), state.toString())
    end InvalidCommand

    sealed trait EventError extends AggregateError

    case class UnhandledEvent(event: AREvent, state: State) extends EventError:
        val userMessage: UserMessage =
            UserMessage("error.unhandled.event", event.toString(), state.toString())

    sealed trait FactoryError extends AggregateError:
        def entityId: String
        def userMessage: UserMessage

    case class EntityAlreadyExists(entityId: String, id: Id) extends FactoryError:
        def userMessage = UserMessage(s"${entityId}.error.entity.exists", id.toString())

    case class EntityNotFound(entityId: String, id: Id) extends FactoryError:
        def userMessage = UserMessage(s"${entityId}.error.entity.not.found", id.toString())

    type ViewProcessor = works.iterative.entity.ViewProcessor[AREvent]

    trait AggregateRootFactory[T <: AggregateRoot]:
        type NotFound = EntityNotFound
        type AlreadyExists = EntityAlreadyExists

        protected def AlreadyExists(id: Id): AlreadyExists =
            EntityAlreadyExists(entityId, id)

        protected def NotFound(id: Id): NotFound =
            EntityNotFound(entityId, id)

        def entityId: String
        def make(id: Id): IO[AlreadyExists, T]
        def load(id: Id): IO[NotFound, T]

        def loadOrMake(id: Id): UIO[T] =
            load(id)
                .orElse(make(id))
                .orDieWith: _ =>
                    new RuntimeException(
                        s"Loading or making entity ${entityId} with id ${id} failed, loading fails with NotFound, making fails with AlreadyExists"
                    )

        def make()(using idGen: IdGenerator[Id]): UIO[T] =
            val tryToCreate =
                for
                    id <- idGen.nextId
                    entity <- make(id)
                yield entity

            tryToCreate
                .retryN(10)
                .orDieWith: _ =>
                    new RuntimeException(
                        "Cannot create entity, generator failed 10 times to create new ID"
                    )
        end make
    end AggregateRootFactory

    trait AggregateRoot:
        def id: Id
        def state: UIO[State]
        def handle(command: ARCommand): IO[AggregateError, ARCommandResult]
    end AggregateRoot

    trait EntityCreateService[Init]:
        type Op[A] = ZIO[CurrentUser, AggregateError, A]

        def create(initData: Init): Op[Id]
    end EntityCreateService

    trait EntityUpdateService:
        type Op[A] = ZIO[CurrentUser, AggregateError, A]

        def update(id: Id, command: Command): Op[Unit]
    end EntityUpdateService

    trait EntityService[Init]
        extends EntityCreateService[Init]
        with EntityUpdateService
    end EntityService

    trait EntityServiceAccessor[R]:
        type Op[A] = ZIO[CurrentUser & R, AggregateError, A]
        inline def delegate[A](inline f: R => ZIO[CurrentUser, AggregateError, A]): Op[A] =
            ZIO.serviceWithZIO[R](f)
    end EntityServiceAccessor
end AggregateRootModule
