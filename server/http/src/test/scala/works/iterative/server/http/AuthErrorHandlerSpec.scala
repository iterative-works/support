// PURPOSE: Test suite for AuthErrorHandler validating authentication error to HTTP status code mapping
// PURPOSE: Ensures Unauthenticated maps to 401, Forbidden maps to 403, with appropriate error messages

package works.iterative.server.http

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.*
import org.http4s.*
import org.http4s.dsl.io.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object AuthErrorHandlerSpec extends ZIOSpecDefault:

  def spec = suite("AuthErrorHandlerSpec")(
    test("Unauthenticated error returns 401 Unauthorized") {
      val error = AuthenticationError.Unauthenticated("No token provided")
      val response = AuthErrorHandler.toResponse(error)

      assertTrue(
        response.status == Status.Unauthorized,
        response.status.code == 401
      )
    },

    test("Forbidden error returns 403 Forbidden") {
      val error = AuthenticationError.Forbidden("document:123", "edit")
      val response = AuthErrorHandler.toResponse(error)

      assertTrue(
        response.status == Status.Forbidden,
        response.status.code == 403
      )
    },

    test("InvalidCredentials error returns 401 Unauthorized") {
      val error = AuthenticationError.InvalidCredentials
      val response = AuthErrorHandler.toResponse(error)

      assertTrue(
        response.status == Status.Unauthorized,
        response.status.code == 401
      )
    },

    test("TokenExpired error returns 401 Unauthorized") {
      val error = AuthenticationError.TokenExpired
      val response = AuthErrorHandler.toResponse(error)

      assertTrue(
        response.status == Status.Unauthorized,
        response.status.code == 401
      )
    },

    test("InvalidToken error returns 401 Unauthorized") {
      val error = AuthenticationError.InvalidToken("Malformed JWT")
      val response = AuthErrorHandler.toResponse(error)

      assertTrue(
        response.status == Status.Unauthorized,
        response.status.code == 401
      )
    },

    test("Forbidden error response includes resource and action") {
      val error = AuthenticationError.Forbidden("document:456", "delete")
      val response = AuthErrorHandler.toResponse(error)

      // Read response body
      val bodyString = response.bodyText.compile.toList.unsafeRunSync().mkString

      // Check body contains expected fields
      assertTrue(
        bodyString.contains("document:456"),
        bodyString.contains("delete"),
        bodyString.contains("error") || bodyString.contains("Forbidden")
      )
    },

    test("Unauthenticated error response includes message") {
      val error = AuthenticationError.Unauthenticated("Session expired")
      val response = AuthErrorHandler.toResponse(error)

      val bodyString = response.bodyText.compile.toList.unsafeRunSync().mkString

      assertTrue(
        bodyString.contains("Session expired") || bodyString.contains("message")
      )
    }
  )
