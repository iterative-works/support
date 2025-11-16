package works.iterative.core.auth
package service

import zio.*

trait AuthenticationService:
    def loggedIn(user: AuthedUserInfo): UIO[Unit] =
        loggedIn(user.token, user.profile)

    def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit]

    def currentUserInfo: UIO[Option[AuthedUserInfo]]

    def currentUser: UIO[Option[BasicProfile]] =
        currentUserInfo.map(_.map(_.profile))

    def currentAccessToken: UIO[Option[AccessToken]] =
        currentUserInfo.map(_.map(_.token))

    def provideCurrentUser[R, E, A](
        effect: ZIO[R & CurrentUser, E, A]
    ): ZIO[R, E | AuthenticationError, A] =
        currentUserInfo.flatMap {
            case Some(info) =>
                effect.provideSome[R](ZLayer.succeed(CurrentUser(info.profile)))
            case None => ZIO.fail(AuthenticationError.missingUser)
        }
end AuthenticationService

object FiberRefAuthentication extends AuthenticationService:
    private val currentUser: FiberRef[Option[AuthedUserInfo]] =
        Unsafe.unsafely(
            FiberRef.unsafe.make(None)
        )

    override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        currentUser.set(Some(AuthedUserInfo(token, profile)))
end FiberRefAuthentication

object GlobalRefAuthentication extends AuthenticationService:
    private val currentUser: Ref[Option[AuthedUserInfo]] =
        Unsafe.unsafely(
            Ref.unsafe.make(None)
        )

    override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        currentUser.set(Some(AuthedUserInfo(token, profile)))
end GlobalRefAuthentication

object AuthenticationService:
    val layer: ZLayer[Any, Nothing, AuthenticationService] =
        ZLayer.succeed(FiberRefAuthentication)

    val global: ZLayer[Any, Nothing, AuthenticationService] =
        ZLayer.succeed(GlobalRefAuthentication)

    def currentAccessToken: URIO[AuthenticationService, Option[AccessToken]] =
        ZIO.serviceWithZIO(_.currentAccessToken)

    def currentUserInfo: URIO[AuthenticationService, Option[AuthedUserInfo]] =
        ZIO.serviceWithZIO(_.currentUserInfo)

    def currentUser: URIO[AuthenticationService, Option[BasicProfile]] =
        ZIO.serviceWithZIO(_.currentUser)

    def loggedIn(
        token: AccessToken,
        profile: BasicProfile
    ): URIO[AuthenticationService, Unit] =
        ZIO.serviceWithZIO(_.loggedIn(token, profile))

    def loggedIn(user: AuthedUserInfo): URIO[AuthenticationService, Unit] =
        ZIO.serviceWithZIO(_.loggedIn(user))

    def provideCurrentUser[R, E, A](
        effect: ZIO[R & CurrentUser, E, A]
    ): ZIO[R & AuthenticationService, E | AuthenticationError, A] =
        ZIO.serviceWithZIO[AuthenticationService](_.provideCurrentUser(effect))
end AuthenticationService
