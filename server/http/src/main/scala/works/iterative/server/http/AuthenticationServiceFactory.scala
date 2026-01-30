// PURPOSE: Factory for creating AuthenticationService based on configuration
// PURPOSE: Selects between TestAuthenticationService (test) and Pac4jAuthenticationAdapter (oidc) with production safety checks
package works.iterative.server.http

import zio.*
import works.iterative.core.auth.service.*
import works.iterative.core.config.{AuthProvider, Environment}
import works.iterative.server.http.impl.pac4j.Pac4jAuthenticationAdapter

/** Factory that creates the appropriate AuthenticationService based on configuration.
  *
  * Reads auth_provider and env from ZIO Config to determine which authentication implementation to
  * use:
  *   - "test": TestAuthenticationService (for testing only, forbidden in production)
  *   - "oidc": Pac4jAuthenticationAdapter (for production OIDC authentication)
  *
  * Example usage:
  * {{{
  *   val app = ZIO.serviceWithZIO[AuthenticationService](_.currentUser)
  *     .provide(AuthenticationServiceFactory.layer)
  * }}}
  */
object AuthenticationServiceFactory:
    /** ZLayer that provides AuthenticationService based on configuration.
      *
      * Validates:
      *   - auth_provider must be set and valid (oidc or test)
      *   - auth_provider=test is forbidden when env=production
      *
      * Logs INFO message indicating which authentication service was selected.
      */
    val layer: ZLayer[Any, Throwable, AuthenticationService] =
        ZLayer.scoped {
            for
                authProvider <- ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
                    .mapError(e => new IllegalArgumentException(e.getMessage))
                env <- ZIO.config(Environment.configDescriptor.nested("env"))
                    .orElseSucceed(Environment.Development)

                _ <- ZIO.when(authProvider == AuthProvider.Test && env == Environment.Production) {
                    ZIO.fail(new IllegalStateException(
                        "auth_provider=test is forbidden in production environment. Use auth_provider=oidc for production."
                    ))
                }

                _ <- ZIO.logInfo(
                    s"Selected authentication provider: ${authProvider.toString.toLowerCase}"
                )

                service <- authProvider match
                    case AuthProvider.Test =>
                        ZIO.logInfo("Using TestAuthenticationService (FAKE AUTHENTICATION)") *>
                            ZIO.succeed(TestAuthenticationService.layer)

                    case AuthProvider.Oidc =>
                        ZIO.logInfo("Using Pac4jAuthenticationAdapter (OIDC)") *>
                            ZIO.succeed(Pac4jAuthenticationAdapter.layer)

                result <- ZIO.service[AuthenticationService].provide(service)
            yield result
        }
end AuthenticationServiceFactory
