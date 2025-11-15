package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.pac4j.core.profile.CommonProfile
import works.iterative.core.auth.*
import works.iterative.core.auth.service.*
import works.iterative.core.*
import scala.jdk.CollectionConverters.*

object Pac4jAuthenticationAdapterSpec extends ZIOSpecDefault:

    def spec = suite("Pac4jAuthenticationAdapter")(
        test("maps Pac4J CommonProfile to BasicProfile with id, name, email") {
            val profile = new CommonProfile()
            profile.setId("test-user-123")
            profile.addAttribute("name", "Test User")
            profile.addAttribute("email", "test@example.com")

            val result = for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]
                basicProfile <- ZIO.succeed(adapter.mapProfile(profile))
            yield basicProfile

            result.map { profile =>
                assert(profile.subjectId)(equalTo(UserId.unsafe("test-user-123"))) &&
                assert(profile.userName)(isSome(equalTo(UserName.unsafe("Test User")))) &&
                assert(profile.email)(isSome(equalTo(Email.unsafe("test@example.com"))))
            }
        },

        test("handles missing email attribute with None") {
            val profile = new CommonProfile()
            profile.setId("test-user-456")
            profile.addAttribute("name", "Test User 2")
            // No email attribute

            val result = for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]
                basicProfile <- ZIO.succeed(adapter.mapProfile(profile))
            yield basicProfile

            result.map { profile =>
                assert(profile.subjectId)(equalTo(UserId.unsafe("test-user-456"))) &&
                assert(profile.email)(isNone)
            }
        },

        test("extracts roles from Pac4J profile attributes") {
            val profile = new CommonProfile()
            profile.setId("test-user-789")
            profile.addAttribute("name", "Admin User")
            profile.addAttribute("roles", java.util.Arrays.asList("admin", "user"))

            val result = for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]
                basicProfile <- ZIO.succeed(adapter.mapProfile(profile))
            yield basicProfile

            result.map { profile =>
                assert(profile.roles)(
                    contains(UserRole.unsafe("admin")) &&
                    contains(UserRole.unsafe("user"))
                )
            }
        },

        test("provideCurrentUser stores user in FiberRef") {
            val profile = new CommonProfile()
            profile.setId("fiber-test-user")
            profile.addAttribute("name", "Fiber Test")
            profile.addAttribute("email", "fiber@test.com")

            val token = AccessToken("test-token-123")

            val result = for
                adapter <- ZIO.service[Pac4jAuthenticationAdapter]
                basicProfile = adapter.mapProfile(profile)
                _ <- adapter.loggedIn(token, basicProfile)
                currentUser <- adapter.currentUserInfo
            yield currentUser

            result.map { userInfo =>
                assert(userInfo)(isSome) &&
                assert(userInfo.map(_.profile.subjectId))(isSome(equalTo(UserId.unsafe("fiber-test-user")))) &&
                assert(userInfo.map(_.token))(isSome(equalTo(token)))
            }
        }
    ).provide(
        Pac4jAuthenticationAdapter.layer
    )

end Pac4jAuthenticationAdapterSpec
