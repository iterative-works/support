package works.iterative.ui.components

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.model.Computable
import zio.*

case class ReloadableComponent[A, I](
    fetch: I => IO[UserMessage, A],
    init: Option[I] = None
)(using runtime: Runtime[Any]):
  private val computable: Var[Computable[A]] = Var(Computable.Uninitialized)
  private val memo: Var[Option[I]] = Var(init)

  val state: Signal[Computable[A]] = computable.signal


  val update: Observer[I] = Observer { input =>
    memo.update(_ => Some(input))
    load(input)
  }

  val reload: Observer[Unit] = Observer(_ => memo.now().foreach(load))

  reload.onNext(())

  def load(input: I): Unit =
    computable.update(_.started)
    // TODO: do we need to manage the result of the run?
    val _ = Unsafe.unsafely {
      runtime.unsafe.runOrFork(
        fetch(input).fold(
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
