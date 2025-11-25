// PURPOSE: Tests for PermissionServiceFactory environment-based service selection
// PURPOSE: Validates configuration-driven instantiation of permission services

package works.iterative.core.auth

import zio.*
import zio.test.*
import works.iterative.core.config.{PermissionServiceType, ValidatedConfig, AuthProvider, Environment}

object PermissionServiceFactorySpec extends ZIOSpecDefault:

  def spec = suite("PermissionServiceFactorySpec")(
    test("PERMISSION_SERVICE=memory returns InMemoryPermissionService") {
      val config = ValidatedConfig(
        authProvider = AuthProvider.Test,
        permissionService = PermissionServiceType.Memory,
        environment = Environment.Development
      )

      for {
        service <- PermissionServiceFactory.make(config)
      } yield assertTrue(service.isInstanceOf[InMemoryPermissionService])
    },

    test("PERMISSION_SERVICE=database fails fast when not implemented") {
      val config = ValidatedConfig(
        authProvider = AuthProvider.Test,
        permissionService = PermissionServiceType.Database,
        environment = Environment.Development
      )

      for {
        exit <- PermissionServiceFactory.make(config).exit
      } yield assertTrue(exit.isFailure)
    },

    // NOTE: DatabasePermissionService doesn't exist yet, so this test is commented out
    // Uncomment when DatabasePermissionService is implemented
    /*
    test("PERMISSION_SERVICE=database returns DatabasePermissionService") {
      val config = ValidatedConfig(
        authProvider = AuthProvider.Test,
        permissionService = PermissionServiceType.Database,
        environment = Environment.Development
      )

      for {
        service <- PermissionServiceFactory.make(config)
      } yield assertTrue(service.isInstanceOf[DatabasePermissionService])
    },
    */
  ).provide(
    ZLayer.succeed(PermissionConfig.default)  // Provide PermissionConfig dependency
  )

end PermissionServiceFactorySpec
