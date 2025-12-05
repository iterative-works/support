package works.iterative.core.auth

import zio.test.*
import works.iterative.core.UserMessage

object AuthenticationErrorSpec extends ZIOSpecDefault:

  def spec = suite("AuthenticationErrorSpec")(
    test("AuthenticationError has Unauthenticated variant") {
      val error = AuthenticationError.Unauthenticated(UserMessage("error.auth.missing_token"))
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
      val error = AuthenticationError.InvalidToken(UserMessage("error.auth.invalid_token"))
      assertTrue(error.isInstanceOf[AuthenticationError])
    },

    test("exhaustive pattern matching on AuthenticationError") {
      val errors = List(
        AuthenticationError.Unauthenticated(UserMessage("test.message")),
        AuthenticationError.Forbidden("resource", "action"),
        AuthenticationError.InvalidCredentials,
        AuthenticationError.TokenExpired,
        AuthenticationError.InvalidToken(UserMessage("error.reason"))
      )

      val result = errors.map {
        case AuthenticationError.Unauthenticated(msg) => s"unauth:${msg.id.value}"
        case AuthenticationError.Forbidden(resource, action) => s"forbidden:$resource:$action"
        case AuthenticationError.InvalidCredentials => "invalid-creds"
        case AuthenticationError.TokenExpired => "expired"
        case AuthenticationError.InvalidToken(msg) => s"invalid:${msg.id.value}"
      }

      assertTrue(
        result.contains("unauth:test.message") &&
        result.contains("forbidden:resource:action") &&
        result.contains("invalid-creds") &&
        result.contains("expired") &&
        result.contains("invalid:error.reason")
      )
    },

    test("missingToken helper returns Unauthenticated with correct message ID") {
      val error = AuthenticationError.missingToken
      error match {
        case AuthenticationError.Unauthenticated(msg) =>
          assertTrue(msg.id.value == "error.auth.missing_token")
        case _ =>
          assertTrue(false)
      }
    },

    test("invalidJwt helper returns InvalidToken with correct message ID") {
      val error = AuthenticationError.invalidJwt("some reason")
      error match {
        case AuthenticationError.InvalidToken(msg) =>
          assertTrue(msg.id.value == "error.auth.invalid_token")
        case _ =>
          assertTrue(false)
      }
    }
  )

end AuthenticationErrorSpec
