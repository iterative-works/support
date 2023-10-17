package works.iterative.tapir

import zio.*
import sttp.tapir.PublicEndpoint
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

opaque type Client[I, E, O] = I => IO[E, O]

object Client:
  def apply[I, E, O](f: I => IO[E, O]): Client[I, E, O] = f

  extension [I, E, O](f: Client[I, E, O])
    def apply(i: I): IO[E, O] = f(i)
    def toEffect: I => ZIO[Any, E, O] = i => f(i)

trait ClientEndpointFactory:
  def umake[I, O](
      endpoint: PublicEndpoint[I, Unit, O, Any]
  ): Client[I, Nothing, O]
  def make[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any]
  ): Client[I, E, O]
  def stream[I, E, O](
      endpoint: PublicEndpoint[
        Unit,
        E,
        ZioStreams.Pipe[I, O],
        ZioStreams & WebSockets
      ]
  ): Client[Unit, E, ZioStreams.Pipe[I, O]]
  def ustream[I, O](
      endpoint: PublicEndpoint[
        Unit,
        Unit,
        ZioStreams.Pipe[I, O],
        ZioStreams & WebSockets
      ]
  ): Client[Unit, Nothing, ZioStreams.Pipe[I, O]]
