// PURPOSE: Maps authentication and authorization errors to appropriate HTTP status codes
// PURPOSE: Converts AuthenticationError enum variants to 401/403 responses with JSON error messages

package works.iterative.server.http

import org.http4s.*
import org.http4s.dsl.io.*
import cats.effect.IO
import works.iterative.core.auth.AuthenticationError
import org.slf4j.LoggerFactory

/** Error handler for authentication and authorization failures.
  *
  * This object provides HTTP4S response mapping for AuthenticationError enum variants.
  * It ensures consistent error responses across the application:
  *
  * - Unauthenticated → 401 Unauthorized
  * - Forbidden → 403 Forbidden
  * - InvalidCredentials → 401 Unauthorized
  * - TokenExpired → 401 Unauthorized
  * - InvalidToken → 401 Unauthorized
  *
  * Usage in HTTP routes:
  * {{{
  *   service.updateDocument(id, title)
  *     .mapError {
  *       case e: AuthenticationError => AuthErrorHandler.toResponse(e)
  *     }
  * }}}
  */
object AuthErrorHandler:

  private val logger = LoggerFactory.getLogger(getClass)

  /** Convert AuthenticationError to HTTP Response.
    *
    * This method pattern matches on the AuthenticationError enum to determine
    * the appropriate HTTP status code and response body. It also logs authentication
    * failures for security monitoring and debugging.
    *
    * @param error The authentication error
    * @return HTTP Response with appropriate status code and JSON body
    */
  def toResponse(error: AuthenticationError): Response[IO] =
    error match
      case AuthenticationError.Unauthenticated(message) =>
        logAuthFailure("unauthenticated", sanitizeMessage(message))
        Response[IO](Status.Unauthorized)
          .withEntity(formatUnauthenticatedError(message))

      case AuthenticationError.Forbidden(resource, action) =>
        logAuthFailure("forbidden", s"resource=$resource action=$action")
        Response[IO](Status.Forbidden)
          .withEntity(formatForbiddenError(resource, action))

      case AuthenticationError.InvalidCredentials =>
        logAuthFailure("invalid_credentials", "login attempt failed")
        Response[IO](Status.Unauthorized)
          .withEntity(formatSimpleError("Invalid credentials provided"))

      case AuthenticationError.TokenExpired =>
        logAuthFailure("token_expired", "expired token presented")
        Response[IO](Status.Unauthorized)
          .withEntity(formatSimpleError("Authentication token has expired"))

      case AuthenticationError.InvalidToken(reason) =>
        logAuthFailure("invalid_token", sanitizeMessage(reason))
        Response[IO](Status.Unauthorized)
          .withEntity(formatSimpleError(s"Invalid token: ${sanitizeMessage(reason)}"))

  /** Format simple error message as JSON.
    *
    * @param message Error message
    * @return JSON string with error field
    */
  private def formatSimpleError(message: String): String =
    s"""{"error": "$message"}"""

  /** Format unauthenticated error as JSON.
    *
    * @param message Error message
    * @return JSON string with message field
    */
  private def formatUnauthenticatedError(message: String): String =
    s"""{"error": "Unauthenticated", "message": "$message"}"""

  /** Format forbidden error as JSON.
    *
    * Includes resource and action information to help clients understand
    * what permission was required.
    *
    * @param resource The resource identifier
    * @param action The attempted action
    * @return JSON string with error, resource, and action fields
    */
  private def formatForbiddenError(resource: String, action: String): String =
    s"""{"error": "Forbidden", "resource": "$resource", "action": "$action"}"""

  /** Log authentication failure for security monitoring.
    *
    * Uses info level logging for visibility without triggering alerts.
    * Authentication failures are expected in normal operation and should
    * be monitored but not treated as errors.
    *
    * @param errorType Type of authentication error
    * @param details Additional context (sanitized)
    */
  private def logAuthFailure(errorType: String, details: String): Unit =
    logger.info(s"Authentication failure: type=$errorType details=$details")

  /** Sanitize message to prevent information leakage.
    *
    * Removes potentially sensitive information from error messages
    * before logging or returning to client. This prevents leaking:
    * - Full token values
    * - Internal system details
    * - Stack traces
    *
    * @param message Original error message
    * @return Sanitized message safe for logging/response
    */
  private def sanitizeMessage(message: String): String =
    // Truncate long messages that might contain tokens
    val maxLength = 100
    val truncated = if message.length > maxLength then
      message.take(maxLength) + "..."
    else
      message

    // Remove common sensitive patterns
    truncated
      .replaceAll("Bearer \\S+", "Bearer [REDACTED]")
      .replaceAll("token=\\S+", "token=[REDACTED]")
      .replaceAll("jwt=\\S+", "jwt=[REDACTED]")

end AuthErrorHandler
