package works.iterative.ui.components

import com.raquo.laminar.api.L.*
import sttp.tapir.PublicEndpoint
import works.iterative.core.*
import works.iterative.tapir.ClientEndpointFactory
import works.iterative.ui.model.Computable
import zio.*
import zio.stream.*
import works.iterative.tapir.ClientErrorConstructor
import works.iterative.tapir.ClientResultConstructor
import works.iterative.tapir.BaseUriExtractor

case class ReloadableComponent[A, I](
    fetch: I => IO[UserMessage, A],
    init: Option[I] = None,
    updates: Option[UStream[ReloadableComponent.Reload[A]]] = None,
    loadSchedule: Schedule[Any, Any, ?] = Schedule.stop
)(using runtime: Runtime[Any]):
    import ReloadableComponent.Reload

    private val computable: Var[Computable[A]] = Var(Computable.Uninitialized)
    private val memo: Var[Option[I]] = Var(init)

    // Set the value manually to avoid the need for a fetch, or prevent glitch
    val setter: Observer[A] = Observer { value =>
        computable.set(Computable.Ready(value))
    }

    val state: Signal[Computable[A]] = computable.signal

    def now(): Option[A] = computable.now().toOption

    val update: Observer[I] = Observer { input =>
        memo.update(_ => Some(input))
        load(input)
    }

    val reload: Observer[Reload[A]] = Observer {
        case Reload.Once => memo.now().foreach(load)
        case Reload.UntilChanged(original) =>
            memo.now().foreach(reloadUntilChanged(_, original))
    }

    private def eventStreamFromZioStream[A](
        eff: UStream[A]
    ): EventStream[A] =
        var runningFiber: Option[Fiber.Runtime[Nothing, Unit]] = None
        EventStream
            .fromCustomSource(
                shouldStart = _ => true,
                start = (fireValue, _, _, _) =>
                    runningFiber = Some(Unsafe.unsafely {
                        runtime.unsafe.fork(
                            eff.runForeach(v => ZIO.succeed(fireValue(v)))
                        )
                    }),
                stop = _ =>
                    runningFiber.foreach { f =>
                        Unsafe.unsafely {
                            runtime.unsafe.fork(f.interrupt)
                        }
                    }
            )
    end eventStreamFromZioStream

    private def updateFromZioStream(
        upd: UStream[Reload[A]]
    ): HtmlMod =
        onMountBind { _ =>
            eventStreamFromZioStream(upd) --> reload
        }

    private def updateStream: HtmlMod = updates match
        case None      => emptyMod
        case Some(upd) => updateFromZioStream(upd)

    def initMod: HtmlMod = modSeq(
        EventStream.fromValue(ReloadableComponent.Reload.Once) --> reload,
        updateStream
    )

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
        computable.update:
            _.started

        // TODO: do we need to manage the result of the run?
        val _ = Unsafe.unsafely {
            runtime.unsafe.runOrFork(
                q(input).foldCauseZIO(
                    c =>
                        c.failureOrCause.fold(
                            msg =>
                                ZIO.succeed:
                                    computable.update:
                                        _.fail(msg)
                            ,
                            t =>
                                for
                                    id <- Random.nextUUID.map(_.toString())
                                    _ <- ZIO.when(c.isDie)(ZIO.logErrorCause(c))
                                    _ <- ZIO.succeed:
                                        computable.update:
                                            _.fail(UserMessage("error.unexpected", id))
                                yield ()
                        ),
                    result =>
                        ZIO.succeed:
                            computable.update:
                                _.update(result)
                )
            )
        }
    end doLoad
end ReloadableComponent

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
        yield new ReloadableComponent(factory.make(endpoint))
end ReloadableComponent
