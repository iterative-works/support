// PURPOSE: Tests for TestAuthenticationService providing fake authentication for testing
// PURPOSE: Validates predefined test users, user switching, and CurrentUser integration
package works.iterative.core.auth
package service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.*

object TestAuthenticationServiceSpec extends ZIOSpecDefault:

    def spec = suite("TestAuthenticationService")(
        test("provides predefined testAdmin user") {
            for
                _ <- TestAuthenticationService.loginAsAdmin()
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("test-admin"),
                user.get.userName == Some(UserName.unsafe("Test Admin")),
                user.get.roles.contains(UserRole.unsafe("admin"))
            )
        },

        test("provides predefined testUser user") {
            for
                _ <- TestAuthenticationService.loginAsUser()
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("test-user"),
                user.get.userName == Some(UserName.unsafe("Test User")),
                user.get.roles.contains(UserRole.unsafe("user"))
            )
        },

        test("provides predefined testViewer user") {
            for
                _ <- TestAuthenticationService.loginAsViewer()
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("test-viewer"),
                user.get.userName == Some(UserName.unsafe("Test Viewer")),
                user.get.roles.contains(UserRole.unsafe("viewer"))
            )
        },

        test("loginAs creates user with specified userId") {
            for
                _ <- TestAuthenticationService.loginAs("custom-user-123")
                user <- TestAuthenticationService.currentUser
            yield assertTrue(
                user.isDefined,
                user.get.subjectId == UserId.unsafe("custom-user-123")
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

        test("CurrentUser is available after login") {
            for
                _ <- TestAuthenticationService.loginAsAdmin()
                userId <- TestAuthenticationService.provideCurrentUser {
                    CurrentUser.use(summon[CurrentUser].subjectId.value)
                }
            yield assertTrue(userId == "test-admin")
        },

        test("logout clears current user") {
            for
                _ <- TestAuthenticationService.loginAsUser()
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
                _ <- TestAuthenticationService.loginAsAdmin()
                user1 <- TestAuthenticationService.currentUser
                _ <- TestAuthenticationService.loginAsViewer()
                user2 <- TestAuthenticationService.currentUser
            yield assertTrue(
                user1.get.subjectId == UserId.unsafe("test-admin"),
                user2.get.subjectId == UserId.unsafe("test-viewer")
            )
        }
    )

end TestAuthenticationServiceSpec
