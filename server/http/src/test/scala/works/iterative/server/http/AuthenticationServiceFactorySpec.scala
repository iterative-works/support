// PURPOSE: Tests for AuthenticationServiceFactory selecting correct authentication implementation
// PURPOSE: Validates AUTH_PROVIDER environment variable handling and production safety checks
package works.iterative.server.http

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.service.*
import works.iterative.server.http.impl.pac4j.Pac4jAuthenticationAdapter

object AuthenticationServiceFactorySpec extends ZIOSpecDefault:

    def spec = suite("AuthenticationServiceFactory")(
        test("AUTH_PROVIDER=test provides TestAuthenticationService") {
            TestSystem.putEnv("AUTH_PROVIDER", "test") *>
            TestSystem.putEnv("APP_ENV", "development") *>
            ZIO.serviceWith[AuthenticationService](service =>
                assertTrue(service == TestAuthenticationService)
            ).provide(AuthenticationServiceFactory.layer)
        },

        test("AUTH_PROVIDER=oidc provides Pac4jAuthenticationAdapter") {
            TestSystem.putEnv("AUTH_PROVIDER", "oidc") *>
            TestSystem.putEnv("APP_ENV", "development") *>
            TestSystem.putEnv("OIDC_CLIENT_ID", "test-client") *>
            TestSystem.putEnv("OIDC_CLIENT_SECRET", "test-secret") *>
            TestSystem.putEnv("OIDC_DISCOVERY_URI", "https://example.com/.well-known/openid-configuration") *>
            ZIO.serviceWith[AuthenticationService](service =>
                assertTrue(service == Pac4jAuthenticationAdapter)
            ).provide(AuthenticationServiceFactory.layer)
        },

        test("AUTH_PROVIDER=test with APP_ENV=production fails with clear error") {
            val effect = TestSystem.putEnv("AUTH_PROVIDER", "test") *>
                TestSystem.putEnv("APP_ENV", "production") *>
                ZIO.service[AuthenticationService].provide(AuthenticationServiceFactory.layer)

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("test") && err.toString.contains("production"),
                    completed = _ => false
                )
            )
        },

        test("missing AUTH_PROVIDER fails with clear error") {
            val effect = ZIO.service[AuthenticationService]
                .provide(AuthenticationServiceFactory.layer)

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("AUTH_PROVIDER"),
                    completed = _ => false
                )
            )
        },

        test("invalid AUTH_PROVIDER value fails with clear error") {
            val effect = TestSystem.putEnv("AUTH_PROVIDER", "invalid") *>
                ZIO.service[AuthenticationService].provide(AuthenticationServiceFactory.layer)

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("invalid") && err.toString.contains("AUTH_PROVIDER"),
                    completed = _ => false
                )
            )
        }
    )

end AuthenticationServiceFactorySpec
