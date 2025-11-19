// PURPOSE: Loads configuration from environment variables (I/O operations)
// PURPOSE: Bridges between system environment and pure validation logic

package works.iterative.infrastructure.config

import works.iterative.core.config.{ConfigValidationError, ConfigValidator, ValidatedConfig}
import zio.*

object ConfigLoader:
  /** Read environment variable with optional default value.
    *
    * @param name Environment variable name
    * @param default Default value if variable is not set
    * @return The value or None
    */
  private def getEnv(name: String, default: Option[String] = None): Option[String] =
    sys.env.get(name).orElse(default)

  /** Load and validate configuration from environment variables.
    *
    * This is the shell layer that performs I/O (reading environment variables)
    * and then delegates to the pure ConfigValidator in the functional core.
    *
    * Environment variables read:
    * - AUTH_PROVIDER (required): oidc | test
    * - PERMISSION_SERVICE (required): memory | database
    * - ENV (required): development | production
    * - OIDC_CLIENT_ID (required for oidc): OIDC client identifier
    * - OIDC_CLIENT_SECRET (required for oidc): OIDC client secret
    * - OIDC_DISCOVERY_URI (required for oidc): OIDC discovery endpoint
    *
    * @return ZIO effect that either fails with validation errors or succeeds with validated config
    */
  def loadConfig(): IO[ConfigValidationError, ValidatedConfig] =
    ZIO.succeed {
      ConfigValidator.validateConfig(
        authProvider = getEnv("AUTH_PROVIDER"),
        permissionService = getEnv("PERMISSION_SERVICE"),
        environment = getEnv("ENV"),
        oidcClientId = getEnv("OIDC_CLIENT_ID"),
        oidcClientSecret = getEnv("OIDC_CLIENT_SECRET"),
        oidcDiscoveryUri = getEnv("OIDC_DISCOVERY_URI")
      )
    }.flatMap {
      case Left(error) => ZIO.fail(error)
      case Right(config) => ZIO.succeed(config)
    }
end ConfigLoader
