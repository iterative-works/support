package works.iterative.tapir

import zio.*
import sttp.tapir.Endpoint
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

/** Create effectful methods to perform the endpoint operation
  *
  * Just a useful way to have something that will derive the client from the endpoint using other
  * layers, like BaseUri and provided STTP Backend.
  *
  * The resulting type is either
  *   - `S => I => IO[E, O]` if the endpoint is secure and error prone
  *   - `S => I => UIO[O]` if the endpoint is secure and infallible
  *   - `I => IO[E, O]` if the endpoint is public and error prone
  *   - `I => UIO[O]` if the endpoint is public and infallible
  */
trait ClientEndpointFactory:
    def make[S, I, E, O](
        endpoint: Endpoint[S, I, E, O, ZioStreams & WebSockets]
    )(using
        b: BaseUriExtractor[O],
        e: ClientErrorConstructor[E],
        m: ClientResultConstructor[S, I, e.Error, O]
    ): m.Result
end ClientEndpointFactory

trait ClientResultConstructor[S, I, E, O]:
    type Result
    def makeResult(effect: S => I => IO[E, O]): Result

object ClientResultConstructor
    extends LowProirityClientResultConstructorImplicits:
    given publicResultConstructor[I, E, O]: ClientResultConstructor[Unit, I, E, O]
    with
        type Result = I => IO[E, O]
        def makeResult(effect: Unit => I => IO[E, O]): Result = effect(())
end ClientResultConstructor

trait LowProirityClientResultConstructorImplicits:
    given secureResultConstructor[S, I, E, O]: ClientResultConstructor[S, I, E, O]
    with
        type Result = S => I => IO[E, O]
        def makeResult(effect: S => I => IO[E, O]): Result = effect
end LowProirityClientResultConstructorImplicits

trait ClientErrorConstructor[-E]:
    type Error
    def mapErrorCause[A](effect: IO[E, A]): IO[Error, A]

object ClientErrorConstructor
    extends LowPriorityClientErrorConstructorImplicits:
    given noErrorConstructor: ClientErrorConstructor[Unit] with
        type Error = Nothing
        def mapErrorCause[A](effect: IO[Unit, A]): IO[Nothing, A] =
            effect.mapErrorCause(e =>
                Cause.die(
                    throw new IllegalStateException(s"Infallible endpoint failure: ${e}")
                )
            )
    end noErrorConstructor
end ClientErrorConstructor

trait LowPriorityClientErrorConstructorImplicits:
    given errorConstructor[E]: ClientErrorConstructor[E] with
        type Error = E
        def mapErrorCause[A](effect: IO[E, A]): IO[E, A] = effect
end LowPriorityClientErrorConstructorImplicits
