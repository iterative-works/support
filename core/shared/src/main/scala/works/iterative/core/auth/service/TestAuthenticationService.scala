// PURPOSE: Provides flexible fake authentication service for testing
// PURPOSE: Allows test code to create users with project-specific roles and switch between them
package works.iterative.core.auth
package service

import zio.*
import works.iterative.core.*

object TestAuthenticationService extends AuthenticationService:
    private val currentUser: FiberRef[Option[AuthedUserInfo]] =
        Unsafe.unsafely(FiberRef.unsafe.make(None))

    override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        currentUser.set(Some(AuthedUserInfo(token, profile)))

    /** Login as a user with specified profile attributes.
      *
      * This is the main method for test authentication. Projects define their own
      * test users with roles/attributes that match their domain.
      *
      * Usage example:
      * {{{
      *   TestAuthenticationService.loginAs(
      *     userId = "user-123",
      *     userName = Some("John Doe"),
      *     email = Some("john@example.com"),
      *     roles = Set("editor", "viewer")
      *   )
      * }}}
      *
      * @param userId User identifier
      * @param userName Optional user display name (defaults to userId if not provided)
      * @param email Optional email address
      * @param roles Set of role names (project-specific)
      * @param avatar Optional avatar URL
      */
    def loginAs(
        userId: String,
        userName: Option[String] = None,
        email: Option[String] = None,
        roles: Set[String] = Set.empty,
        avatar: Option[String] = None
    ): UIO[Unit] =
        val profile = BasicProfile(
            subjectId = UserId.unsafe(userId),
            userName = userName.map(UserName.unsafe).orElse(Some(UserName.unsafe(userId))),
            email = email.map(Email.unsafe),
            avatar = avatar.map(Avatar.unsafe),
            roles = roles.map(UserRole.unsafe)
        )
        val token = AccessToken(s"test-token-$userId")
        loggedIn(token, profile)

    /** Login with a fully constructed BasicProfile.
      *
      * Useful when you need complete control over the profile or want to
      * define test user constants in your project's test utilities.
      *
      * Usage example:
      * {{{
      *   val testEditor = BasicProfile(
      *     subjectId = UserId.unsafe("editor-1"),
      *     userName = Some(UserName.unsafe("Jane Editor")),
      *     email = Some(Email.unsafe("jane@example.com")),
      *     avatar = None,
      *     roles = Set(UserRole.unsafe("editor"))
      *   )
      *   TestAuthenticationService.loginWithProfile(testEditor)
      * }}}
      *
      * @param profile The user profile to authenticate
      */
    def loginWithProfile(profile: BasicProfile): UIO[Unit] =
        val token = AccessToken(s"test-token-${profile.subjectId.value}")
        loggedIn(token, profile)

    /**
     * Clear the current user (logout).
     */
    def logout(): UIO[Unit] =
        currentUser.set(None)

    /**
     * ZLayer providing TestAuthenticationService backed by FiberRef.
     *
     * WARNING: Logs a warning message on initialization to ensure developers know
     * they're using test authentication. Fails if used in production.
     */
    val layer: ZLayer[Any, Throwable, AuthenticationService] =
        ZLayer.scoped {
            for
                appEnv <- System.env("APP_ENV").map(_.getOrElse("development"))
                _ <- ZIO.when(appEnv.toLowerCase == "production") {
                    ZIO.fail(new IllegalStateException(
                        "TestAuthenticationService cannot be used in production. " +
                        "This is a security violation."
                    ))
                }
                _ <- ZIO.logWarning("TestAuthenticationService instantiated - FAKE AUTHENTICATION FOR TESTING ONLY")
                _ <- ZIO.logError("SECURITY: This service should never be used in production")
            yield TestAuthenticationService
        }

end TestAuthenticationService
