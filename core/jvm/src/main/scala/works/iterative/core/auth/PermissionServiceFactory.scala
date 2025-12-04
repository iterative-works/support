// PURPOSE: Factory for creating permission service instances based on environment configuration
// PURPOSE: Selects between InMemoryPermissionService and DatabasePermissionService using ZIO Config

package works.iterative.core.auth

import zio.*
import works.iterative.core.config.PermissionServiceType

object PermissionServiceFactory:

  /** Create a PermissionService based on configuration.
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
    * @param serviceType The type of permission service to create
    * @return ZIO effect that creates the appropriate PermissionService
    */
  def make(serviceType: PermissionServiceType): URIO[PermissionConfig, PermissionService] =
    serviceType match
      case PermissionServiceType.Memory =>
        ZIO.logInfo("Loading InMemoryPermissionService") *>
        ZIO.serviceWith[PermissionConfig] { permConfig =>
          InMemoryPermissionService.make(permConfig)
        }.flatten

      case PermissionServiceType.Database =>
        ZIO.dieMessage("DatabasePermissionService not yet implemented. Configure PERMISSION_SERVICE=memory instead.")

  /** ZLayer factory for PermissionService using ZIO Config.
    *
    * Reads PERMISSION_SERVICE from configuration and creates the appropriate
    * PermissionService implementation.
    *
    * Requires PermissionConfig in the environment.
    *
    * Example usage:
    * {{{
    *   val app = ZLayer.make[PermissionService](
    *     PermissionServiceFactory.layer,
    *     PermissionConfig.layer
    *   )
    * }}}
    */
  val layer: ZLayer[PermissionConfig, Config.Error, PermissionService] =
    ZLayer.fromZIO {
      for {
        serviceType <- ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        service <- make(serviceType)
      } yield service
    }

end PermissionServiceFactory
