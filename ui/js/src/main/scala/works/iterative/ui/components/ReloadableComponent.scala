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
    loadSchedule: Schedule[Any, Any, ?] = Schedule.stop
)(using runtime: Runtime[Any]):
  private val computable: Var[Computable[A]] = Var(Computable.Uninitialized)
  private val memo: Var[Option[I]] = Var(init)

  val state: Signal[Computable[A]] = computable.signal

  def now(): Option[A] = computable.now().toOption

  val update: Observer[I] = Observer { input =>
    memo.update(_ => Some(input))
    load(input)
  }

  val reload: Observer[ReloadableComponent.Reload[A]] = Observer {
    case ReloadableComponent.Reload.Once => memo.now().foreach(load)
    case ReloadableComponent.Reload.UntilChanged(original) =>
      memo.now().foreach(reloadUntilChanged(_, original))
  }

  def initMod: HtmlMod =
    EventStream.fromValue(ReloadableComponent.Reload.Once) --> reload

  def load(input: I): Unit = doLoad(
    input,
    fetch(_).retry(loadSchedule)
  )

  def reloadUntilChanged(input: I, original: A): Unit =
    doLoad(
      input,
      fetch(_)
        .repeat(loadSchedule && Schedule.recurWhile(_ == original))
        .map(_._2)
    )

  private def doLoad(
      input: I,
      q: I => IO[UserMessage, A]
  ): Unit =
    computable.update(_.started)
    // TODO: do we need to manage the result of the run?
    val _ = Unsafe.unsafely {
      runtime.unsafe.runOrFork(
        q(input).fold(
          msg => computable.update(_.fail(msg)),
          result => computable.update(_.update(result))
        )
      )
    }

object ReloadableComponent:
  enum Reload[+A]:
    case Once extends Reload[Nothing]
    case UntilChanged(a: A) extends Reload[A]

  def apply[A](fetch: IO[UserMessage, A])(using
      runtime: Runtime[Any]
  ): ReloadableComponent[A, Unit] =
    ReloadableComponent(_ => fetch, Some(()))

  def apply[A, I](
      endpoint: PublicEndpoint[I, Unit, A, Any]
  ): URIO[ClientEndpointFactory, ReloadableComponent[A, I]] =
    for
      given Runtime[Any] <- ZIO.runtime[Any]
      factory <- ZIO.service[ClientEndpointFactory]
    yield new ReloadableComponent(factory.umake(endpoint).toEffect)
