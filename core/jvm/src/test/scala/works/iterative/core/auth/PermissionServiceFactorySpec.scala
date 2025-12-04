// PURPOSE: Tests for PermissionServiceFactory environment-based service selection
// PURPOSE: Validates configuration-driven instantiation of permission services

package works.iterative.core.auth

import zio.*
import zio.test.*
import works.iterative.core.config.PermissionServiceType

object PermissionServiceFactorySpec extends ZIOSpecDefault:

  def spec = suite("PermissionServiceFactorySpec")(
    test("PERMISSION_SERVICE=memory returns InMemoryPermissionService") {
      for {
        service <- PermissionServiceFactory.make(PermissionServiceType.Memory)
      } yield assertTrue(service.isInstanceOf[InMemoryPermissionService])
    },

    test("PERMISSION_SERVICE=database fails fast when not implemented") {
      for {
        exit <- PermissionServiceFactory.make(PermissionServiceType.Database).exit
      } yield assertTrue(exit.isFailure)
    },

    test("layer reads config and creates InMemoryPermissionService") {
      for {
        service <- ZIO.service[PermissionService]
      } yield assertTrue(service.isInstanceOf[InMemoryPermissionService])
    }.provide(
        PermissionServiceFactory.layer,
        ZLayer.succeed(PermissionConfig.default)
      ) @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "memory"))),

    test("layer fails with clear error for invalid config") {
      val testEffect = ZIO.scoped {
        PermissionServiceFactory.layer.build
      }.provide(
        ZLayer.succeed(PermissionConfig.default)
      )

      for {
        exit <- testEffect.exit
      } yield assertTrue(exit.isFailure)
    } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "invalid")))
  ).provide(
    ZLayer.succeed(PermissionConfig.default)
  )

end PermissionServiceFactorySpec
