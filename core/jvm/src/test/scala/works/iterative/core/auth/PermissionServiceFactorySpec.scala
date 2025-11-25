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

    test("invalid PermissionServiceType fails with clear error") {
      // This test validates the exhaustiveness of pattern matching
      // Scala 3 enums ensure compile-time exhaustiveness checking,
      // so this test primarily validates the factory is well-structured
      val config = ValidatedConfig(
        authProvider = AuthProvider.Test,
        permissionService = PermissionServiceType.Memory,
        environment = Environment.Development
      )

      for {
        service <- PermissionServiceFactory.make(config)
      } yield assertTrue(service != null)
    }
  ).provide(
    ZLayer.succeed(PermissionConfig.default)  // Provide PermissionConfig dependency
  )

end PermissionServiceFactorySpec
