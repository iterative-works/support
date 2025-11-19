// PURPOSE: Validates application configuration on startup
// PURPOSE: Accumulates all validation errors and provides clear error messages

package works.iterative.core.config

/** Error collecting all configuration validation failures */
case class ConfigValidationError(errors: List[String]):
  def formatted: String =
    "Configuration validation failed:\n" + errors.map(e => s"  - $e").mkString("\n")
end ConfigValidationError

/** Validated configuration values */
case class ValidatedConfig(
    authProvider: AuthProvider,
    permissionService: PermissionServiceType,
    environment: Environment
)

enum AuthProvider:
  case Oidc
  case Test

enum PermissionServiceType:
  case Memory
  case Database

enum Environment:
  case Development
  case Production

object ConfigValidator:
  /** Validate application configuration.
    *
    * Accumulates all validation errors before returning, making it easy to fix
    * all configuration issues at once rather than one at a time.
    *
    * Example valid configurations:
    *
    * Development with test authentication:
    * {{{
    *   validateConfig(
    *     authProvider = Some("test"),
    *     permissionService = Some("memory"),
    *     environment = Some("development"),
    *     oidcClientId = None,
    *     oidcClientSecret = None,
    *     oidcDiscoveryUri = None
    *   )
    * }}}
    *
    * Production with OIDC authentication:
    * {{{
    *   validateConfig(
    *     authProvider = Some("oidc"),
    *     permissionService = Some("database"),
    *     environment = Some("production"),
    *     oidcClientId = Some("my-client-id"),
    *     oidcClientSecret = Some("my-secret"),
    *     oidcDiscoveryUri = Some("https://auth.example.com/.well-known/openid-configuration")
    *   )
    * }}}
    *
    * @param authProvider Auth provider value (oidc | test)
    * @param permissionService Permission service type (memory | database)
    * @param environment Environment (development | production)
    * @param oidcClientId OIDC client ID (required for oidc)
    * @param oidcClientSecret OIDC client secret (required for oidc)
    * @param oidcDiscoveryUri OIDC discovery URI (required for oidc)
    * @return Either validation errors or validated config
    */
  def validateConfig(
      authProvider: Option[String],
      permissionService: Option[String],
      environment: Option[String],
      oidcClientId: Option[String],
      oidcClientSecret: Option[String],
      oidcDiscoveryUri: Option[String]
  ): Either[ConfigValidationError, ValidatedConfig] =
    val errors = collection.mutable.ListBuffer[String]()

    // Validate AUTH_PROVIDER
    val authProviderValue = authProvider match
      case None =>
        errors += "AUTH_PROVIDER is required (valid values: oidc, test)"
        None
      case Some(value) =>
        value.toLowerCase match
          case "oidc" => Some(AuthProvider.Oidc)
          case "test" => Some(AuthProvider.Test)
          case _ =>
            errors += s"AUTH_PROVIDER '$value' is invalid (valid values: oidc, test)"
            None

    // Validate PERMISSION_SERVICE
    val permissionServiceValue = permissionService match
      case None =>
        errors += "PERMISSION_SERVICE is required (valid values: memory, database)"
        None
      case Some(value) =>
        value.toLowerCase match
          case "memory"   => Some(PermissionServiceType.Memory)
          case "database" => Some(PermissionServiceType.Database)
          case _ =>
            errors += s"PERMISSION_SERVICE '$value' is invalid (valid values: memory, database)"
            None

    // Validate ENV
    val environmentValue = environment match
      case None =>
        errors += "ENV is required (valid values: development, production)"
        None
      case Some(value) =>
        value.toLowerCase match
          case "development" => Some(Environment.Development)
          case "production"  => Some(Environment.Production)
          case _ =>
            errors += s"ENV '$value' is invalid (valid values: development, production)"
            None

    // Cross-validation: test provider forbidden in production
    (authProviderValue, environmentValue) match
      case (Some(AuthProvider.Test), Some(Environment.Production)) =>
        errors += "AUTH_PROVIDER 'test' cannot be used in production environment"
      case _ => ()

    // Cross-validation: OIDC requires credentials
    authProviderValue match
      case Some(AuthProvider.Oidc) =>
        if oidcClientId.isEmpty then
          errors += "OIDC_CLIENT_ID is required when AUTH_PROVIDER is 'oidc'"
        if oidcClientSecret.isEmpty then
          errors += "OIDC_CLIENT_SECRET is required when AUTH_PROVIDER is 'oidc'"
        if oidcDiscoveryUri.isEmpty then
          errors += "OIDC_DISCOVERY_URI is required when AUTH_PROVIDER is 'oidc'"
      case _ => ()

    // Return result
    if errors.isEmpty then
      Right(
        ValidatedConfig(
          authProvider = authProviderValue.get,
          permissionService = permissionServiceValue.get,
          environment = environmentValue.get
        )
      )
    else Left(ConfigValidationError(errors.toList))
  end validateConfig
end ConfigValidator
