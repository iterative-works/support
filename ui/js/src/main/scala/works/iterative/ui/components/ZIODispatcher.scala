package works.iterative.ui.components

import zio.*
import com.raquo.airstream.core.EventStream
import works.iterative.ui.components.laminar.LaminarExtensions.*
import works.iterative.core.auth.CurrentUser
import works.iterative.core.auth.BasicProfile
import com.raquo.airstream.core.Signal
import works.iterative.core.auth.UserProfile
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.state.Var

trait ZIODispatcher[+Env]:
    def dispatchAction(action: ZIO[Env, Nothing, Unit]): Unit
    def dispatchUserAction(action: ZIO[Env & CurrentUser, Nothing, Unit]): Unit
    def dispatchStream[A](action: ZIO[Env, Nothing, A]): EventStream[A]
    def dispatchUserStream[A](
        action: ZIO[Env & CurrentUser, Nothing, A]
    ): EventStream[A]
end ZIODispatcher

object ZIODispatcher:

    def fromRuntime[Env](
        userSignal: Signal[Option[UserProfile]],
        handleUnauthenticated: => ZIO[Env, Nothing, Unit]
    )(using
        runtime: Runtime[Env],
        owner: Owner
    ): ZIODispatcher[Env] =
        new ZIODispatcher[Env]:
            private val currentUser: Signal[Option[CurrentUser]] =
                userSignal.map(_.map(u => CurrentUser(BasicProfile(u))))

            private val cuVar = Var[Option[CurrentUser]](None)

            val _ = currentUser.foreach(cuVar.writer.onNext(_))

            override def dispatchAction(action: ZIO[Env, Nothing, Unit]): Unit =
                Unsafe.unsafe(implicit unsafe =>
                    // TODO: do I need to cancel this on evenstream stop?
                    val _ = runtime.unsafe.runToFuture(action)
                )

            override def dispatchUserAction(
                action: ZIO[Env & CurrentUser, Nothing, Unit]
            ): Unit =
                val updatedAction: ZIO[Env, Nothing, Unit] =
                    for
                        cu <- ZIO.attempt(cuVar.now()).orDie
                        _ <- ZIO
                            .fromOption(cu)
                            .foldZIO(
                                _ => handleUnauthenticated,
                                u => action.provideSomeLayer[Env](ZLayer.succeed(u))
                            )
                    yield ()

                dispatchAction(updatedAction)
            end dispatchUserAction

            override def dispatchStream[A](
                action: ZIO[Env, Nothing, A]
            ): EventStream[A] =
                action.toEventStream

            override def dispatchUserStream[A](
                action: ZIO[Env & CurrentUser, Nothing, A]
            ): EventStream[A] =
                val updatedAction: ZIO[Env, Nothing, Option[A]] =
                    for
                        cu <- ZIO.attempt(cuVar.now()).orDie
                        result <- ZIO
                            .fromOption(cu)
                            .foldZIO(
                                _ => handleUnauthenticated.as(None),
                                u => action.provideSomeLayer[Env](ZLayer.succeed(u)).asSome
                            )
                    yield result

                updatedAction.toEventStream.collectSome
            end dispatchUserStream

    def empty[R] = new ZIODispatcher[R]:
        override def dispatchAction(action: ZIO[R, Nothing, Unit]): Unit = ()
        override def dispatchUserAction(
            action: ZIO[R & CurrentUser, Nothing, Unit]
        ): Unit = ()
        override def dispatchStream[A](action: ZIO[R, Nothing, A]): EventStream[A] =
            EventStream.empty
        override def dispatchUserStream[A](
            action: ZIO[R & CurrentUser, Nothing, A]
        ): EventStream[A] = EventStream.empty
end ZIODispatcher
