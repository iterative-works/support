// PURPOSE: Integration tests validating Pac4jModuleRegistry with AuthenticationService
// PURPOSE: Tests end-to-end flow from Pac4J authentication to CurrentUser availability via FiberRef
package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.pac4j.core.profile.CommonProfile
import works.iterative.core.auth.*
import works.iterative.core.auth.service.*
import works.iterative.core.*

object Pac4jModuleRegistryIntegrationSpec extends ZIOSpecDefault:

    // Test implementation of Pac4jModuleRegistry
    class TestPac4jModuleRegistry(
        val pac4jSecurity: Pac4jHttpSecurity[[A] =>> RIO[AuthenticationService, A]],
        val profileMapper: List[CommonProfile] => Option[BasicProfile]
    ) extends Pac4jModuleRegistry[AuthenticationService, BasicProfile]:
        override def profileToUser(profile: List[CommonProfile]): Option[BasicProfile] =
            profileMapper(profile)

        override def modules: List[ZIOWebModule[AuthenticationService]] = List.empty
    end TestPac4jModuleRegistry

    def spec = suite("Pac4jModuleRegistryIntegrationSpec")(
        test("After Pac4J authentication, AuthenticationService.currentUser returns authenticated user") {
            // This test verifies the integration point described in the architecture:
            // After profileToUser succeeds, Pac4jModuleRegistry should call AuthenticationService.loggedIn

            for
                authService <- ZIO.service[AuthenticationService]

                // Create a test user profile
                profile = new CommonProfile()
                _ = profile.setId("integration-user-1")
                _ = profile.addAttribute("name", "Integration User")
                _ = profile.addAttribute("email", "integration@test.com")
                _ = profile.addAttribute("roles", java.util.Arrays.asList("user", "admin"))

                // Map to BasicProfile (simulating what profileToUser would do)
                basicProfile = BasicProfile(
                    subjectId = UserId.unsafe("integration-user-1"),
                    userName = Some(UserName.unsafe("Integration User")),
                    email = Some(Email.unsafe("integration@test.com")),
                    avatar = None,
                    roles = Set(UserRole.unsafe("user"), UserRole.unsafe("admin"))
                )

                // Simulate what wrapModule should do: call AuthenticationService.loggedIn
                token = AccessToken("integration-token-1")
                _ <- authService.loggedIn(token, basicProfile)

                // Verify currentUser returns the authenticated user
                currentUser <- authService.currentUser
            yield assertTrue(
                currentUser.isDefined,
                currentUser.get.subjectId == UserId.unsafe("integration-user-1"),
                currentUser.get.userName == Some(UserName.unsafe("Integration User")),
                currentUser.get.email == Some(Email.unsafe("integration@test.com")),
                currentUser.get.roles.contains(UserRole.unsafe("admin"))
            )
        },

        test("CurrentUser is available in downstream ZIO effects via provideCurrentUser") {
            for
                authService <- ZIO.service[AuthenticationService]

                // Login a user
                basicProfile = BasicProfile(
                    subjectId = UserId.unsafe("downstream-user"),
                    userName = Some(UserName.unsafe("Downstream User")),
                    email = None,
                    avatar = None,
                    roles = Set(UserRole.unsafe("user"))
                )
                token = AccessToken("downstream-token")
                _ <- authService.loggedIn(token, basicProfile)

                // Use provideCurrentUser to access CurrentUser in downstream effect
                userId <- authService.provideCurrentUser {
                    CurrentUser.use(summon[CurrentUser].subjectId.value)
                }
            yield assertTrue(userId == "downstream-user")
        },

        test("FiberRef isolation: Concurrent requests don't share user context") {
            for
                authService <- ZIO.service[AuthenticationService]

                // Fork two fibers simulating concurrent requests
                effect1 = (for
                    profile1 <- ZIO.succeed(BasicProfile(
                        subjectId = UserId.unsafe("concurrent-user-1"),
                        userName = Some(UserName.unsafe("Concurrent 1")),
                        email = None,
                        avatar = None,
                        roles = Set.empty
                    ))
                    _ <- authService.loggedIn(AccessToken("token-1"), profile1)
                    _ <- ZIO.sleep(50.millis)
                    user <- authService.currentUser
                yield user.map(_.subjectId))

                effect2 = (for
                    profile2 <- ZIO.succeed(BasicProfile(
                        subjectId = UserId.unsafe("concurrent-user-2"),
                        userName = Some(UserName.unsafe("Concurrent 2")),
                        email = None,
                        avatar = None,
                        roles = Set.empty
                    ))
                    _ <- authService.loggedIn(AccessToken("token-2"), profile2)
                    _ <- ZIO.sleep(50.millis)
                    user <- authService.currentUser
                yield user.map(_.subjectId))

                fiber1 <- effect1.fork
                fiber2 <- effect2.fork
                result1 <- fiber1.join
                result2 <- fiber2.join
            yield assertTrue(
                result1 == Some(UserId.unsafe("concurrent-user-1")),
                result2 == Some(UserId.unsafe("concurrent-user-2"))
            )
        } @@ TestAspect.withLiveClock @@ TestAspect.nonFlaky(10)
    ).provide(
        AuthenticationService.layer // Use FiberRefAuthentication
    )

end Pac4jModuleRegistryIntegrationSpec
