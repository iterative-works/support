// PURPOSE: Factory for creating permission service instances based on environment configuration
// PURPOSE: Selects between InMemoryPermissionService and DatabasePermissionService using Scala 3 enum

package works.iterative.core.auth

import zio.*
import works.iterative.core.config.{ValidatedConfig, PermissionServiceType}

object PermissionServiceFactory:

  /** Create a PermissionService based on validated configuration.
    *
    * This factory uses the PermissionServiceType enum to select the appropriate
    * implementation:
    * - PermissionServiceType.Memory => InMemoryPermissionService (for development/testing)
    * - PermissionServiceType.Database => DatabasePermissionService (for production)
    *
    * The factory requires PermissionConfig to be provided in the environment.
    *
    * Configuration:
    * - Set PERMISSION_SERVICE=memory for in-memory service
    * - Set PERMISSION_SERVICE=database for database-backed service
    *
    * @param config Validated application configuration
    * @return ZIO effect that creates the appropriate PermissionService
    */
  def make(config: ValidatedConfig): URIO[PermissionConfig, PermissionService] =
    config.permissionService match
      case PermissionServiceType.Memory =>
        ZIO.logInfo("Loading InMemoryPermissionService") *>
        ZIO.serviceWith[PermissionConfig] { permConfig =>
          InMemoryPermissionService.make(permConfig)
        }.flatten

      case PermissionServiceType.Database =>
        // TODO: DatabasePermissionService not yet implemented
        // For now, fall back to InMemoryPermissionService with a warning
        ZIO.logWarning("DatabasePermissionService not yet implemented, falling back to InMemoryPermissionService") *>
        ZIO.serviceWith[PermissionConfig] { permConfig =>
          InMemoryPermissionService.make(permConfig)
        }.flatten

  /** ZLayer factory for PermissionService based on ValidatedConfig.
    *
    * Requires ValidatedConfig and PermissionConfig in the environment.
    * Provides PermissionService.
    *
    * Example usage:
    * {{{
    *   val app = ZLayer.make[PermissionService](
    *     PermissionServiceFactory.layer,
    *     ValidatedConfig.layer,
    *     PermissionConfig.layer
    *   )
    * }}}
    */
  val layer: ZLayer[ValidatedConfig & PermissionConfig, Nothing, PermissionService] =
    ZLayer.fromZIO {
      for {
        config <- ZIO.service[ValidatedConfig]
        service <- make(config)
      } yield service
    }

end PermissionServiceFactory
