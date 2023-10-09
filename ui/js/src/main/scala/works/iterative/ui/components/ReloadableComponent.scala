package works.iterative.ui.components

import com.raquo.laminar.api.L.*
import sttp.tapir.PublicEndpoint
import works.iterative.core.*
import works.iterative.tapir.ClientEndpointFactory
import works.iterative.ui.model.Computable
import zio.*

case class ReloadableComponent[A, I](
    fetch: I => IO[UserMessage, A],
    init: Option[I] = None,
    loadSchedule: Schedule[Any, UserMessage, Any] = Schedule.stop
)(using runtime: Runtime[Any]):
  private val computable: Var[Computable[A]] = Var(Computable.Uninitialized)
  private val memo: Var[Option[I]] = Var(init)

  val state: Signal[Computable[A]] = computable.signal


  val update: Observer[I] = Observer { input =>
    memo.update(_ => Some(input))
    load(input)
  }

  val reload: Observer[Unit] = Observer(_ => memo.now().foreach(load))

  def initMod: HtmlMod = EventStream.fromValue(()) --> reload

  def load(input: I): Unit =
    computable.update(_.started)
    // TODO: do we need to manage the result of the run?
    val _ = Unsafe.unsafely {
      runtime.unsafe.runOrFork(
        fetch(input).retry(loadSchedule).fold(
          msg => computable.update(_.fail(msg)),
          result => computable.update(_.update(result))
        )
      )
    }

object ReloadableComponent:
  def apply[A](fetch: IO[UserMessage, A])(using
      runtime: Runtime[Any]
  ): ReloadableComponent[A, Unit] =
    ReloadableComponent(_ => fetch, Some(()))

  def apply[A, I](endpoint: PublicEndpoint[I, Unit, A, Any]): URIO[ClientEndpointFactory, ReloadableComponent[A, I]] =
    for
      given Runtime[Any] <- ZIO.runtime[Any]
      factory <- ZIO.service[ClientEndpointFactory]
    yield new ReloadableComponent(factory.umake(endpoint).toEffect)
