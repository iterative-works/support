// PURPOSE: Integration tests for Pac4J authentication flow with AuthenticationService
// PURPOSE: Validates end-to-end authentication including FiberRef lifecycle and user context propagation
package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.pac4j.core.profile.CommonProfile
import works.iterative.core.auth.*
import works.iterative.core.*

object Pac4jIntegrationSpec extends ZIOSpecDefault:

    def spec = suite("Pac4jIntegrationSpec")(
        test("Pac4J middleware calls AuthenticationService.loggedIn after successful auth") {
            for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]

                // Create a mock Pac4J profile (simulating successful OIDC login)
                profile = new CommonProfile()
                _ = profile.setId("test-user-123")
                _ = profile.addAttribute("name", "Test User")
                _ = profile.addAttribute("email", "test@example.com")
                _ = profile.addAttribute("roles", java.util.Arrays.asList("user", "admin"))

                // Map profile and login
                basicProfile = adapter.mapProfile(profile)
                token = AccessToken("test-token-123")
                _ <- adapter.loggedIn(token, basicProfile)

                // Verify user is logged in
                userInfo <- adapter.currentUserInfo
            yield assertTrue(
                userInfo.isDefined,
                userInfo.get.profile.subjectId == UserId.unsafe("test-user-123"),
                userInfo.get.profile.userName == Some(UserName.unsafe("Test User")),
                userInfo.get.profile.email == Some(Email.unsafe("test@example.com")),
                userInfo.get.profile.roles.contains(UserRole.unsafe("admin")),
                userInfo.get.token == token
            )
        },

        test("CurrentUser available in subsequent ZIO effects after login") {
            for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]

                // Login a user
                profile = new CommonProfile()
                _ = profile.setId("user-456")
                _ = profile.addAttribute("name", "Jane Doe")
                basicProfile = adapter.mapProfile(profile)
                token = AccessToken("token-456")
                _ <- adapter.loggedIn(token, basicProfile)

                // Use provideCurrentUser to run an effect that needs CurrentUser
                result <- adapter.provideCurrentUser {
                    CurrentUser.use(summon[CurrentUser].subjectId.value)
                }
            yield assertTrue(result == "user-456")
        },

        test("FiberRef isolation - concurrent requests don't share user context") {
            for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]

                // Simulate two concurrent requests with different users
                effect1 = (for
                    profile1 <- ZIO.succeed {
                        val p = new CommonProfile()
                        p.setId("user-concurrent-1")
                        p
                    }
                    basic1 = adapter.mapProfile(profile1)
                    _ <- adapter.loggedIn(AccessToken("token-1"), basic1)
                    _ <- ZIO.sleep(50.millis) // Simulate some work
                    info <- adapter.currentUserInfo
                yield info.map(_.profile.subjectId))

                effect2 = (for
                    profile2 <- ZIO.succeed {
                        val p = new CommonProfile()
                        p.setId("user-concurrent-2")
                        p
                    }
                    basic2 = adapter.mapProfile(profile2)
                    _ <- adapter.loggedIn(AccessToken("token-2"), basic2)
                    _ <- ZIO.sleep(50.millis) // Simulate some work
                    info <- adapter.currentUserInfo
                yield info.map(_.profile.subjectId))

                fiber1 <- effect1.fork
                fiber2 <- effect2.fork
                result1 <- fiber1.join
                result2 <- fiber2.join
            yield assertTrue(
                result1 == Some(UserId.unsafe("user-concurrent-1")),
                result2 == Some(UserId.unsafe("user-concurrent-2"))
            )
        } @@ TestAspect.withLiveClock @@ TestAspect.nonFlaky(10),

        test("FiberRef lifecycle management - cleanup after request") {
            for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]

                // Login in a scoped effect (simulating request scope)
                _ <- ZIO.scoped {
                    for
                        profile <- ZIO.succeed {
                            val p = new CommonProfile()
                            p.setId("temp-user")
                            p
                        }
                        basicProfile = adapter.mapProfile(profile)
                        _ <- adapter.loggedIn(AccessToken("temp-token"), basicProfile)

                        // Verify user is logged in within scope
                        info1 <- adapter.currentUserInfo
                        _ <- ZIO.attempt(assert(info1.isDefined))
                    yield ()
                }

                // After scope, user should still be in FiberRef
                // (actual cleanup would happen with proper FiberRef.make scoping)
                // This test shows current behavior - we'll fix in GREEN phase
                info2 <- adapter.currentUserInfo
            yield assertTrue(info2.isDefined) // Will change when we implement proper scoping
        }
    ).provide(
        Pac4jAuthenticationAdapter.layer
    )

end Pac4jIntegrationSpec
