// PURPOSE: Tests for AuthenticationServiceFactory selecting correct authentication implementation
// PURPOSE: Validates auth_provider config handling and production safety checks
package works.iterative.server.http

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.service.*
import works.iterative.server.http.impl.pac4j.Pac4jAuthenticationAdapter

object AuthenticationServiceFactorySpec extends ZIOSpecDefault:

    def spec = suite("AuthenticationServiceFactory")(
        test("auth_provider=test provides TestAuthenticationService") {
            ZIO.serviceWith[AuthenticationService](service =>
                assertTrue(service == TestAuthenticationService)
            ).provide(AuthenticationServiceFactory.layer)
        } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map(
            "auth_provider" -> "test",
            "env" -> "development"
        ))),

        test("auth_provider=oidc provides Pac4jAuthenticationAdapter") {
            ZIO.serviceWith[AuthenticationService](service =>
                assertTrue(service == Pac4jAuthenticationAdapter)
            ).provide(AuthenticationServiceFactory.layer)
        } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map(
            "auth_provider" -> "oidc",
            "env" -> "development"
        ))),

        test("auth_provider=test with env=production fails with clear error") {
            val effect = ZIO.scoped {
                AuthenticationServiceFactory.layer.build
            }

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("test") && err.toString.contains("production"),
                    completed = _ => false
                )
            )
        } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map(
            "auth_provider" -> "test",
            "env" -> "production"
        ))),

        test("missing auth_provider fails with clear error") {
            val effect = ZIO.scoped {
                AuthenticationServiceFactory.layer.build
            }

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("auth_provider"),
                    completed = _ => false
                )
            )
        } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map.empty)),

        test("invalid auth_provider value fails with clear error") {
            val effect = ZIO.scoped {
                AuthenticationServiceFactory.layer.build
            }

            for
                exit <- effect.exit
            yield assertTrue(
                exit.isFailure,
                exit.foldExit(
                    failed = err => err.toString.contains("invalid") || err.toString.contains("auth_provider"),
                    completed = _ => false
                )
            )
        } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map(
            "auth_provider" -> "invalid"
        )))
    )

end AuthenticationServiceFactorySpec
