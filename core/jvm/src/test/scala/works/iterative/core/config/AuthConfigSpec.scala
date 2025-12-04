// PURPOSE: Test suite for AuthConfig enums and config descriptors
// PURPOSE: Verifies ZIO Config integration and error messages

package works.iterative.core.config

import zio.*
import zio.test.*

object AuthConfigSpec extends ZIOSpecDefault:

  def spec = suite("AuthConfig")(
    suite("AuthProvider")(
      test("parses 'oidc' (case-insensitive)") {
        for {
          result <- ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
        } yield assertTrue(result == AuthProvider.Oidc)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("auth_provider" -> "oidc"))),

      test("parses 'OIDC' uppercase") {
        for {
          result <- ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
        } yield assertTrue(result == AuthProvider.Oidc)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("auth_provider" -> "OIDC"))),

      test("parses 'test' (case-insensitive)") {
        for {
          result <- ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
        } yield assertTrue(result == AuthProvider.Test)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("auth_provider" -> "test"))),

      test("parses 'TEST' uppercase") {
        for {
          result <- ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
        } yield assertTrue(result == AuthProvider.Test)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("auth_provider" -> "TEST"))),

      test("fails for invalid value") {
        val effect = ZIO.config(AuthProvider.configDescriptor.nested("auth_provider"))
        for {
          exit <- effect.exit
        } yield assertTrue(exit.isFailure)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("auth_provider" -> "invalid")))
    ),

    suite("PermissionServiceType")(
      test("parses 'memory' (case-insensitive)") {
        for {
          result <- ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        } yield assertTrue(result == PermissionServiceType.Memory)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "memory"))),

      test("parses 'MEMORY' uppercase") {
        for {
          result <- ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        } yield assertTrue(result == PermissionServiceType.Memory)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "MEMORY"))),

      test("parses 'database'") {
        for {
          result <- ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        } yield assertTrue(result == PermissionServiceType.Database)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "database"))),

      test("fails for invalid value") {
        val effect = ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        for {
          exit <- effect.exit
        } yield assertTrue(exit.isFailure)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("permission_service" -> "redis")))
    ),

    suite("Environment")(
      test("parses 'development' (case-insensitive)") {
        for {
          result <- ZIO.config(Environment.configDescriptor.nested("env"))
        } yield assertTrue(result == Environment.Development)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("env" -> "development"))),

      test("parses 'DEVELOPMENT' uppercase") {
        for {
          result <- ZIO.config(Environment.configDescriptor.nested("env"))
        } yield assertTrue(result == Environment.Development)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("env" -> "DEVELOPMENT"))),

      test("parses 'production'") {
        for {
          result <- ZIO.config(Environment.configDescriptor.nested("env"))
        } yield assertTrue(result == Environment.Production)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("env" -> "production"))),

      test("fails for invalid value") {
        val effect = ZIO.config(Environment.configDescriptor.nested("env"))
        for {
          exit <- effect.exit
        } yield assertTrue(exit.isFailure)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map("env" -> "staging")))
    ),

    suite("missing config")(
      test("fails when key is missing") {
        val effect = ZIO.config(PermissionServiceType.configDescriptor.nested("permission_service"))
        for {
          exit <- effect.exit
        } yield assertTrue(exit.isFailure)
      } @@ TestAspect.withConfigProvider(ConfigProvider.fromMap(Map.empty))
    )
  )
