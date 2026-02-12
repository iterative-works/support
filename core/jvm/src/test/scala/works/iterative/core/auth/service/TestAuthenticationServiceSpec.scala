// PURPOSE: Tests for TestAuthenticationService providing fake authentication for testing
// PURPOSE: Validates flexible user creation, user switching, and CurrentUser integration
package works.iterative.core.auth
package service

import zio.*
import zio.test.*

import works.iterative.core.*

object TestAuthenticationServiceSpec extends ZIOSpecDefault:

    def spec = suite("TestAuthenticationService")(
        test("loginAs with roles creates user with specified roles") {
            for
                _ <- TestAuthenticationService.loginAs(
                    userId = "admin-123",
                    userName = Some("Admin User"),
                    roles = Set("admin", "user")
                )
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("admin-123"),
                user.get.userName == Some(UserName.unsafe("Admin User")),
                user.get.roles.contains(UserRole.unsafe("admin")),
                user.get.roles.contains(UserRole.unsafe("user"))
            )
        },

        test("loginAs with userName and email creates complete profile") {
            for
                _ <- TestAuthenticationService.loginAs(
                    userId = "user-456",
                    userName = Some("Regular User"),
                    email = Some("user@example.com"),
                    roles = Set("viewer")
                )
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("user-456"),
                user.get.userName == Some(UserName.unsafe("Regular User")),
                user.get.email == Some(Email.unsafe("user@example.com")),
                user.get.roles.contains(UserRole.unsafe("viewer"))
            )
        },

        test("loginAs with minimal parameters defaults userName to userId") {
            for
                _ <- TestAuthenticationService.loginAs(userId = "minimal-user")
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("minimal-user"),
                user.get.userName == Some(UserName.unsafe("minimal-user")),
                user.get.email.isEmpty,
                user.get.roles.isEmpty
            )
        },

        test("loginAs with custom roles supports project-specific roles") {
            for
                _ <- TestAuthenticationService.loginAs(
                    userId = "editor-789",
                    roles = Set("editor", "publisher", "content-manager")
                )
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.roles.size == 3,
                user.get.roles.contains(UserRole.unsafe("editor")),
                user.get.roles.contains(UserRole.unsafe("publisher")),
                user.get.roles.contains(UserRole.unsafe("content-manager"))
            )
        },

        test("loginAs creates synthetic AccessToken") {
            for
                _ <- TestAuthenticationService.loginAs("token-test-user")
                token <- TestAuthenticationService.currentAccessToken
            yield assertTrue(
                token.isDefined,
                token.get.token.contains("token-test-user")
            )
        },

        test("loginWithProfile allows complete control over profile") {
            val customProfile = BasicProfile(
                subjectId = UserId.unsafe("custom-123"),
                userName = Some(UserName.unsafe("Custom User")),
                email = Some(Email.unsafe("custom@example.com")),
                avatar = Some(Avatar.unsafe("https://example.com/avatar.jpg")),
                roles = Set(UserRole.unsafe("special-role"))
            )
            for
                _ <- TestAuthenticationService.loginWithProfile(customProfile)
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("custom-123"),
                user.get.avatar == Some(Avatar.unsafe("https://example.com/avatar.jpg"))
            )
        },

        test("CurrentUser is available after login") {
            for
                _ <- TestAuthenticationService.loginAs(userId = "current-test", roles = Set("tester"))
                userId <- TestAuthenticationService.provideCurrentUser {
                    CurrentUser.use(summon[CurrentUser].subjectId.value)
                }
            yield assertTrue(userId == "current-test")
        },

        test("logout clears current user") {
            for
                _ <- TestAuthenticationService.loginAs("logout-test")
                user1 <- TestAuthenticationService.currentUser
                _ <- TestAuthenticationService.logout()
                user2 <- TestAuthenticationService.currentUser
            yield assertTrue(
                user1.isDefined,
                user2.isEmpty
            )
        },

        test("can switch between users") {
            for
                _ <- TestAuthenticationService.loginAs("user-1", roles = Set("role-1"))
                user1 <- TestAuthenticationService.currentUser
                _ <- TestAuthenticationService.loginAs("user-2", roles = Set("role-2"))
                user2 <- TestAuthenticationService.currentUser
            yield assertTrue(
                user1.get.subjectId == UserId.unsafe("user-1"),
                user1.get.roles.contains(UserRole.unsafe("role-1")),
                user2.get.subjectId == UserId.unsafe("user-2"),
                user2.get.roles.contains(UserRole.unsafe("role-2"))
            )
        }
    )

end TestAuthenticationServiceSpec
