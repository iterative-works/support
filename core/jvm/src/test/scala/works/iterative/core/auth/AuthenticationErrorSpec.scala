package works.iterative.core.auth

import zio.test.*

object AuthenticationErrorSpec extends ZIOSpecDefault:

  def spec = suite("AuthenticationErrorSpec")(
    test("AuthenticationError has Unauthenticated variant") {
      val error = AuthenticationError.Unauthenticated("No token provided")
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("AuthenticationError has Forbidden variant") {
      val error = AuthenticationError.Forbidden("document:123", "edit")
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("AuthenticationError has InvalidCredentials variant") {
      val error = AuthenticationError.InvalidCredentials
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("AuthenticationError has TokenExpired variant") {
      val error = AuthenticationError.TokenExpired
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("AuthenticationError has InvalidToken variant") {
      val error = AuthenticationError.InvalidToken("Malformed JWT")
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("exhaustive pattern matching on AuthenticationError") {
      val errors = List(
        AuthenticationError.Unauthenticated("test"),
        AuthenticationError.Forbidden("resource", "action"),
        AuthenticationError.InvalidCredentials,
        AuthenticationError.TokenExpired,
        AuthenticationError.InvalidToken("reason")
      )

      val result = errors.map {
        case AuthenticationError.Unauthenticated(msg) => s"unauth:$msg"
        case AuthenticationError.Forbidden(resource, action) => s"forbidden:$resource:$action"
        case AuthenticationError.InvalidCredentials => "invalid-creds"
        case AuthenticationError.TokenExpired => "expired"
        case AuthenticationError.InvalidToken(reason) => s"invalid:$reason"
      }

      assertTrue(
        result.contains("unauth:test") &&
        result.contains("forbidden:resource:action") &&
        result.contains("invalid-creds") &&
        result.contains("expired") &&
        result.contains("invalid:reason")
      )
    }
  )

end AuthenticationErrorSpec
