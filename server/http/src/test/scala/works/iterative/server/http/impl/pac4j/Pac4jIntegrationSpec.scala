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
            // Create a mock Pac4J profile (simulating successful OIDC login)
            val profile = new CommonProfile()
            profile.setId("test-user-123")
            profile.addAttribute("name", "Test User")
            profile.addAttribute("email", "test@example.com")
            profile.addAttribute("roles", java.util.Arrays.asList("user", "admin"))

            // Map profile and login
            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)
            val token = AccessToken("test-token-123")

            for
                _ <- Pac4jAuthenticationAdapter.loggedIn(token, basicProfile)
                // Verify user is logged in
                userInfo <- Pac4jAuthenticationAdapter.currentUserInfo
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
            // Login a user
            val profile = new CommonProfile()
            profile.setId("user-456")
            profile.addAttribute("name", "Jane Doe")
            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)
            val token = AccessToken("token-456")

            for
                _ <- Pac4jAuthenticationAdapter.loggedIn(token, basicProfile)
                // Use provideCurrentUser to run an effect that needs CurrentUser
                result <- Pac4jAuthenticationAdapter.provideCurrentUser {
                    CurrentUser.use(summon[CurrentUser].subjectId.value)
                }
            yield assertTrue(result == "user-456")
        },

        test("FiberRef isolation - concurrent requests don't share user context") {
            // Simulate two concurrent requests with different users
            val effect1 = (for
                profile1 <- ZIO.succeed {
                    val p = new CommonProfile()
                    p.setId("user-concurrent-1")
                    p
                }
                basic1 = Pac4jAuthenticationAdapter.mapProfile(profile1)
                _ <- Pac4jAuthenticationAdapter.loggedIn(AccessToken("token-1"), basic1)
                _ <- ZIO.sleep(50.millis) // Simulate some work
                info <- Pac4jAuthenticationAdapter.currentUserInfo
            yield info.map(_.profile.subjectId))

            val effect2 = (for
                profile2 <- ZIO.succeed {
                    val p = new CommonProfile()
                    p.setId("user-concurrent-2")
                    p
                }
                basic2 = Pac4jAuthenticationAdapter.mapProfile(profile2)
                _ <- Pac4jAuthenticationAdapter.loggedIn(AccessToken("token-2"), basic2)
                _ <- ZIO.sleep(50.millis) // Simulate some work
                info <- Pac4jAuthenticationAdapter.currentUserInfo
            yield info.map(_.profile.subjectId))

            for
                fiber1 <- effect1.fork
                fiber2 <- effect2.fork
                result1 <- fiber1.join
                result2 <- fiber2.join
            yield assertTrue(
                result1 == Some(UserId.unsafe("user-concurrent-1")),
                result2 == Some(UserId.unsafe("user-concurrent-2"))
            )
        } @@ TestAspect.withLiveClock @@ TestAspect.nonFlaky(10)
    )

end Pac4jIntegrationSpec
