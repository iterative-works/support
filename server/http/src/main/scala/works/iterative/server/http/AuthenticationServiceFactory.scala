// PURPOSE: Factory for creating AuthenticationService based on AUTH_PROVIDER environment variable
// PURPOSE: Selects between TestAuthenticationService (test) and Pac4jAuthenticationAdapter (oidc) with production safety checks
package works.iterative.server.http

import zio.*
import works.iterative.core.auth.service.*
import works.iterative.server.http.impl.pac4j.Pac4jAuthenticationAdapter

/**
 * Authentication provider types supported by the factory.
 */
enum AuthProvider:
    case Oidc, Test

object AuthProvider:
    def fromString(value: String): Either[String, AuthProvider] = value.toLowerCase match
        case "oidc" => Right(AuthProvider.Oidc)
        case "test" => Right(AuthProvider.Test)
        case _ => Left(s"Invalid AUTH_PROVIDER value: '$value'. Supported values: oidc, test")
end AuthProvider

/**
 * Factory that creates the appropriate AuthenticationService based on configuration.
 *
 * Reads AUTH_PROVIDER environment variable to determine which authentication implementation to use:
 * - "test": TestAuthenticationService (for testing only, forbidden in production)
 * - "oidc": Pac4jAuthenticationAdapter (for production OIDC authentication)
 *
 * Example usage:
 * {{{
 *   val app = ZIO.serviceWithZIO[AuthenticationService](_.currentUser)
 *     .provide(AuthenticationServiceFactory.layer)
 * }}}
 */
object AuthenticationServiceFactory:
    /**
     * ZLayer that provides AuthenticationService based on AUTH_PROVIDER environment variable.
     *
     * Validates:
     * - AUTH_PROVIDER must be set
     * - AUTH_PROVIDER must be valid (oidc or test)
     * - AUTH_PROVIDER=test is forbidden when APP_ENV=production
     *
     * Logs INFO message indicating which authentication service was selected.
     */
    val layer: ZLayer[Any, Throwable, AuthenticationService] =
        ZLayer.scoped {
            for
                authProviderStr <- System.env("AUTH_PROVIDER")
                    .someOrFail(new IllegalStateException("AUTH_PROVIDER environment variable is required"))

                appEnv <- System.env("APP_ENV").map(_.getOrElse("development"))

                authProvider <- ZIO.fromEither(AuthProvider.fromString(authProviderStr))
                    .mapError(err => new IllegalArgumentException(err))

                _ <- ZIO.when(authProvider == AuthProvider.Test && appEnv.toLowerCase == "production") {
                    ZIO.fail(new IllegalStateException(
                        "AUTH_PROVIDER=test is forbidden in production environment. " +
                        "Use AUTH_PROVIDER=oidc for production."
                    ))
                }

                _ <- ZIO.logInfo(s"Selected authentication provider: ${authProvider.toString.toLowerCase}")

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
