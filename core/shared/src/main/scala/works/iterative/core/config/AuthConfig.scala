// PURPOSE: Authentication and authorization configuration types using ZIO Config
// PURPOSE: Provides type-safe enums with config descriptors for ZIO Config integration

package works.iterative.core.config

import zio.Config

/** Authentication provider type.
  *
  * Configuration key: `auth_provider`
  * Valid values: "oidc", "test" (case-insensitive)
  *
  * Example:
  * {{{
  * // Environment variable
  * AUTH_PROVIDER=oidc
  *
  * // HOCON
  * auth_provider = "test"
  *
  * // Usage with ZIO Config
  * ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
  * }}}
  */
enum AuthProvider:
  case Oidc
  case Test

object AuthProvider:
  val configDescriptor: Config[AuthProvider] =
    Config.string.mapOrFail {
      case s if s.equalsIgnoreCase("oidc") => Right(AuthProvider.Oidc)
      case s if s.equalsIgnoreCase("test") => Right(AuthProvider.Test)
      case other => Left(Config.Error.InvalidData(message = s"Invalid auth provider '$other' (valid values: oidc, test)"))
    }

/** Permission service implementation type.
  *
  * Configuration key: `permission_service`
  * Valid values: "memory", "database" (case-insensitive)
  *
  * Example:
  * {{{
  * // Environment variable
  * PERMISSION_SERVICE=memory
  *
  * // HOCON
  * permission_service = "database"
  *
  * // Usage with ZIO Config
  * ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
  * }}}
  */
enum PermissionServiceType:
  case Memory
  case Database

object PermissionServiceType:
  val configDescriptor: Config[PermissionServiceType] =
    Config.string.mapOrFail {
      case s if s.equalsIgnoreCase("memory")   => Right(PermissionServiceType.Memory)
      case s if s.equalsIgnoreCase("database") => Right(PermissionServiceType.Database)
      case other => Left(Config.Error.InvalidData(message = s"Invalid permission service '$other' (valid values: memory, database)"))
    }

/** Environment type for deployment context.
  *
  * Configuration key: `env`
  * Valid values: "development", "production" (case-insensitive)
  *
  * Example:
  * {{{
  * // Environment variable
  * ENV=production
  *
  * // HOCON
  * env = "development"
  *
  * // Usage with ZIO Config
  * ZIO.config(Environment.configDescriptor.nested("env"))
  * }}}
  */
enum Environment:
  case Development
  case Production

object Environment:
  val configDescriptor: Config[Environment] =
    Config.string.mapOrFail {
      case s if s.equalsIgnoreCase("development") => Right(Environment.Development)
      case s if s.equalsIgnoreCase("production")  => Right(Environment.Production)
      case other => Left(Config.Error.InvalidData(message = s"Invalid environment '$other' (valid values: development, production)"))
    }
