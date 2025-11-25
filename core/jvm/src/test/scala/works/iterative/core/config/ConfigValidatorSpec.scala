// PURPOSE: Test suite for ConfigValidator
// PURPOSE: Verifies configuration validation catches errors and provides clear messages

package works.iterative.core.config

import zio.test.*
import zio.test.Assertion.*

object ConfigValidatorSpec extends ZIOSpecDefault:
  def spec = suite("ConfigValidator")(
    test("missing AUTH_PROVIDER fails validation"):
      val result = ConfigValidator.validateConfig(
        authProvider = None,
        permissionService = Some("Memory"),
        environment = Some("development"),
        oidcClientId = None,
        oidcClientSecret = None,
        oidcDiscoveryUri = None
      )
      assertTrue(
        result.isLeft && result.left.exists(_.errors.exists(_.contains("AUTH_PROVIDER")))
      )
    ,
    test("invalid AUTH_PROVIDER enum value fails with clear message"):
      val result = ConfigValidator.validateConfig(
        authProvider = Some("InvalidProvider"),
        permissionService = Some("Memory"),
        environment = Some("development"),
        oidcClientId = None,
        oidcClientSecret = None,
        oidcDiscoveryUri = None
      )
      assertTrue(
        result.isLeft && result.left.exists(_.errors.exists(e =>
          e.contains("AUTH_PROVIDER") && e.contains("InvalidProvider")
        ))
      )
    ,
    test("AUTH_PROVIDER=test forbidden in production environment"):
      val result = ConfigValidator.validateConfig(
        authProvider = Some("test"),
        permissionService = Some("Memory"),
        environment = Some("production"),
        oidcClientId = None,
        oidcClientSecret = None,
        oidcDiscoveryUri = None
      )
      assertTrue(
        result.isLeft && result.left.exists(_.errors.exists(e =>
          e.contains("test") && e.contains("production")
        ))
      )
    ,
    test("OIDC auth provider requires OIDC_CLIENT_ID"):
      val result = ConfigValidator.validateConfig(
        authProvider = Some("oidc"),
        permissionService = Some("Memory"),
        environment = Some("development"),
        oidcClientId = None,
        oidcClientSecret = Some("secret"),
        oidcDiscoveryUri = Some("https://example.com")
      )
      assertTrue(
        result.isLeft && result.left.exists(_.errors.exists(_.contains("OIDC_CLIENT_ID")))
      )
    ,
    test("validation collects ALL errors not just first"):
      val result = ConfigValidator.validateConfig(
        authProvider = Some("InvalidProvider"),
        permissionService = Some("InvalidService"),
        environment = Some("development"),
        oidcClientId = None,
        oidcClientSecret = None,
        oidcDiscoveryUri = None
      )
      assertTrue(
        result.isLeft && result.left.exists(err =>
          err.errors.length >= 2 &&
          err.errors.exists(_.contains("AUTH_PROVIDER")) &&
          err.errors.exists(_.contains("PERMISSION_SERVICE"))
        )
      )
    ,
    test("valid configuration passes"):
      val result = ConfigValidator.validateConfig(
        authProvider = Some("test"),
        permissionService = Some("memory"),
        environment = Some("development"),
        oidcClientId = None,
        oidcClientSecret = None,
        oidcDiscoveryUri = None
      )
      assertTrue(result.isRight)
  )
