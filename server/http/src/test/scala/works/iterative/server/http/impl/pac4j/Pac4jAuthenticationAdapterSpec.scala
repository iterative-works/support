package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.pac4j.core.profile.CommonProfile
import works.iterative.core.auth.*
import works.iterative.core.*

object Pac4jAuthenticationAdapterSpec extends ZIOSpecDefault:

    def spec = suite("Pac4jAuthenticationAdapter")(
        test("maps Pac4J CommonProfile to BasicProfile with id, name, email") {
            val profile = new CommonProfile()
            profile.setId("test-user-123")
            profile.addAttribute("name", "Test User")
            profile.addAttribute("email", "test@example.com")

            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)

            assertTrue(
                basicProfile.subjectId == UserId.unsafe("test-user-123"),
                basicProfile.userName == Some(UserName.unsafe("Test User")),
                basicProfile.email == Some(Email.unsafe("test@example.com"))
            )
        },

        test("handles missing email attribute with None") {
            val profile = new CommonProfile()
            profile.setId("test-user-456")
            profile.addAttribute("name", "Test User 2")
            // No email attribute

            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)

            assertTrue(
                basicProfile.subjectId == UserId.unsafe("test-user-456"),
                basicProfile.email.isEmpty
            )
        },

        test("extracts roles from Pac4J profile attributes") {
            val profile = new CommonProfile()
            profile.setId("test-user-789")
            profile.addAttribute("name", "Admin User")
            profile.addAttribute("roles", java.util.Arrays.asList("admin", "user"))

            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)

            assertTrue(
                basicProfile.roles.contains(UserRole.unsafe("admin")),
                basicProfile.roles.contains(UserRole.unsafe("user"))
            )
        },

        test("provideCurrentUser stores user in FiberRef") {
            val profile = new CommonProfile()
            profile.setId("fiber-test-user")
            profile.addAttribute("name", "Fiber Test")
            profile.addAttribute("email", "fiber@test.com")

            val token = AccessToken("test-token-123")
            val basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)

            for
                _ <- Pac4jAuthenticationAdapter.loggedIn(token, basicProfile)
                currentUser <- Pac4jAuthenticationAdapter.currentUserInfo
            yield assertTrue(
                currentUser.isDefined,
                currentUser.map(_.profile.subjectId).contains(UserId.unsafe("fiber-test-user")),
                currentUser.map(_.token).contains(token)
            )
        }
    )

end Pac4jAuthenticationAdapterSpec
