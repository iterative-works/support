// PURPOSE: Provides fake authentication service for testing with predefined test users
// PURPOSE: Allows test code to switch between test users and access CurrentUser context
package works.iterative.core.auth
package service

import zio.*
import works.iterative.core.*

/**
 * Test authentication service providing predefined test users and easy user switching.
 *
 * WARNING: This service is for testing only and should never be used in production.
 *
 * Example usage:
 * {{{
 *   test("admin can access protected resource") {
 *     for
 *       auth <- ZIO.service[TestAuthenticationService]
 *       _ <- auth.loginAsAdmin()
 *       result <- auth.provideCurrentUser {
 *         protectedOperation()
 *       }
 *     yield assertTrue(result.isSuccess)
 *   }.provide(TestAuthenticationService.layer)
 * }}}
 */
trait TestAuthenticationService extends AuthenticationService:
    /**
     * Login as a user with the specified userId.
     * Creates a BasicProfile with the given userId and a synthetic access token.
     */
    def loginAs(userId: String): UIO[Unit]

    /**
     * Login as the predefined test admin user (test-admin).
     */
    def loginAsAdmin(): UIO[Unit]

    /**
     * Login as the predefined test user (test-user).
     */
    def loginAsUser(): UIO[Unit]

    /**
     * Login as the predefined test viewer user (test-viewer).
     */
    def loginAsViewer(): UIO[Unit]

    /**
     * Clear the current user (logout).
     */
    def logout(): UIO[Unit]
end TestAuthenticationService

object TestAuthenticationService:
    /**
     * Predefined test admin user with admin role.
     */
    val testAdmin: BasicProfile = BasicProfile(
        subjectId = UserId.unsafe("test-admin"),
        userName = Some(UserName.unsafe("Test Admin")),
        email = Some(Email.unsafe("admin@test.example")),
        avatar = None,
        roles = Set(UserRole.unsafe("admin"), UserRole.unsafe("user"))
    )

    /**
     * Predefined test user with user role.
     */
    val testUser: BasicProfile = BasicProfile(
        subjectId = UserId.unsafe("test-user"),
        userName = Some(UserName.unsafe("Test User")),
        email = Some(Email.unsafe("user@test.example")),
        avatar = None,
        roles = Set(UserRole.unsafe("user"))
    )

    /**
     * Predefined test viewer user with viewer role.
     */
    val testViewer: BasicProfile = BasicProfile(
        subjectId = UserId.unsafe("test-viewer"),
        userName = Some(UserName.unsafe("Test Viewer")),
        email = Some(Email.unsafe("viewer@test.example")),
        avatar = None,
        roles = Set(UserRole.unsafe("viewer"))
    )

    /**
     * ZLayer providing TestAuthenticationService backed by FiberRef.
     *
     * WARNING: Logs a warning message on initialization to ensure developers know
     * they're using test authentication.
     */
    val layer: ZLayer[Any, Nothing, TestAuthenticationService] =
        ZLayer.scoped {
            ZIO.logWarning("TestAuthenticationService instantiated - FAKE AUTHENTICATION FOR TESTING ONLY") *>
            ZIO.logError("SECURITY: This service should never be used in production") *>
            ZIO.succeed(TestAuthenticationServiceLive)
        }

    private object TestAuthenticationServiceLive extends TestAuthenticationService:
        private val currentUser: FiberRef[Option[AuthedUserInfo]] =
            Unsafe.unsafely(
                FiberRef.unsafe.make(None)
            )

        override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

        override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
            currentUser.set(Some(AuthedUserInfo(token, profile)))

        override def loginAs(userId: String): UIO[Unit] =
            val profile = BasicProfile(
                subjectId = UserId.unsafe(userId),
                userName = Some(UserName.unsafe(userId)),
                email = None,
                avatar = None,
                roles = Set.empty
            )
            val token = AccessToken(s"test-token-$userId")
            loggedIn(token, profile)

        override def loginAsAdmin(): UIO[Unit] =
            loggedIn(AccessToken("test-token-admin"), testAdmin)

        override def loginAsUser(): UIO[Unit] =
            loggedIn(AccessToken("test-token-user"), testUser)

        override def loginAsViewer(): UIO[Unit] =
            loggedIn(AccessToken("test-token-viewer"), testViewer)

        override def logout(): UIO[Unit] =
            currentUser.set(None)
    end TestAuthenticationServiceLive
end TestAuthenticationService
