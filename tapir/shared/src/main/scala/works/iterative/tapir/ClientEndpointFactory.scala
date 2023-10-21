package works.iterative.tapir

import zio.*
import sttp.tapir.Endpoint
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

import scala.compiletime.{erasedValue, summonFrom}

opaque type Client[I, E, O] = I => IO[E, O]

object Client:
  def apply[I, E, O](f: I => IO[E, O]): Client[I, E, O] = f

  extension [I, E, O](f: Client[I, E, O])
    def apply(i: I): IO[E, O] = f(i)
    def toEffect: I => ZIO[Any, E, O] = i => f(i)

type ClientError[E] = E match
  case Unit => Nothing
  case _    => E

object ClientError:
  inline def cause[E](e: Cause[E]): Cause[ClientError[E]] =
    erasedValue[E] match
      case _: Unit =>
        Cause.die(throw new IllegalStateException("Internal Server Error"))
      case _ => e.asInstanceOf[Cause[ClientError[E]]]

  inline def apply[S, I, E, A](
      client: SecureClient[S, I, E, A]
  ): SecureClient[S, I, ClientError[E], A] =
    s => i => client(s)(i).mapErrorCause(cause(_))

opaque type SecureClient[S, I, E, O] = S => I => IO[E, O]

object SecureClient:
  def apply[S, I, E, O](f: S => I => IO[E, O]): SecureClient[S, I, E, O] = f

  extension [S, I, E, O](f: SecureClient[S, I, E, O])
    def apply(s: S): Client[I, E, O] = f(s)
    def toEffect: S => I => ZIO[Any, E, O] = s => i => f(s)(i)

type ClientResult[S, I, E, O] = S match
  case Unit => I => IO[ClientError[E], O]
  case _    => S => I => IO[ClientError[E], O]

/** Create effectful methods to perform the endpoint operation
  *
  * Just a useful way to have something that will derive the client from the
  * endpoint using other layers, like BaseUri and provided STTP Backend.
  */
trait ClientEndpointFactory:
  inline def makeSecure[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => IO[ClientError[E], O] =
    inline val isWebSocket =
      erasedValue[O] match
        case _: ZioStreams.Pipe[I, O] => true
        case _                        => false

    s => i => makeSecureClient(endpoint, isWebSocket)(s)(i).mapErrorCause(ClientError.cause(_))

  transparent inline def make[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): ClientResult[S, I, E, O] =
    erasedValue[S] match
      case _: Unit => makeSecure(endpoint)(().asInstanceOf[S])
      case _ => makeSecure(endpoint)

  def makeSecureClient[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, ZioStreams & WebSockets],
      isWebSocket: Boolean = false
  ): S => I => IO[E, O]
