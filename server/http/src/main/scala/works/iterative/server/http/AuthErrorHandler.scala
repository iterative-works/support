// PURPOSE: Maps authentication and authorization errors to appropriate HTTP status codes
// PURPOSE: Converts AuthenticationError enum variants to 401/403 responses with JSON error messages

package works.iterative.server.http

import org.http4s.*
import org.http4s.dsl.io.*
import cats.effect.IO
import works.iterative.core.auth.AuthenticationError
import works.iterative.core.UserMessage
import org.slf4j.LoggerFactory

/** Error handler for authentication and authorization failures.
  *
  * This object provides HTTP4S response mapping for AuthenticationError enum variants. It ensures
  * consistent error responses across the application:
  *
  *   - Unauthenticated → 401 Unauthorized
  *   - Forbidden → 403 Forbidden
  *   - InvalidCredentials → 401 Unauthorized
  *   - TokenExpired → 401 Unauthorized
  *   - InvalidToken → 401 Unauthorized
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
      * This method pattern matches on the AuthenticationError enum to determine the appropriate
      * HTTP status code and response body. It also logs authentication failures for security
      * monitoring and debugging.
      *
      * @param error
      *   The authentication error
      * @return
      *   HTTP Response with appropriate status code and JSON body
      */
    def toResponse(error: AuthenticationError): Response[IO] =
        error match
            case AuthenticationError.Unauthenticated(message) =>
                logAuthFailure("unauthenticated", message.toString)
                Response[IO](Status.Unauthorized)
                    .withEntity(formatUnauthenticatedError(message))

            case AuthenticationError.Forbidden(resource, action) =>
                logAuthFailure("forbidden", s"resource=$resource action=$action")
                Response[IO](Status.Forbidden)
                    .withEntity(formatForbiddenError(resource, action))

            case AuthenticationError.InvalidCredentials =>
                logAuthFailure("invalid_credentials", "login attempt failed")
                Response[IO](Status.Unauthorized)
                    .withEntity(formatSimpleError(UserMessage("error.auth.invalid_credentials")))

            case AuthenticationError.TokenExpired =>
                logAuthFailure("token_expired", "expired token presented")
                Response[IO](Status.Unauthorized)
                    .withEntity(formatSimpleError(UserMessage("error.auth.token_expired")))

            case AuthenticationError.InvalidToken(message) =>
                logAuthFailure("invalid_token", message.toString)
                Response[IO](Status.Unauthorized)
                    .withEntity(formatSimpleError(message))

    /** Format simple error message as JSON.
      *
      * @param message
      *   User message with translation ID
      * @return
      *   JSON string with messageId field
      */
    private def formatSimpleError(message: UserMessage): String =
        s"""{"messageId": "${message.id.value}"}"""

    /** Format unauthenticated error as JSON.
      *
      * @param message
      *   User message with translation ID
      * @return
      *   JSON string with error and messageId fields
      */
    private def formatUnauthenticatedError(message: UserMessage): String =
        s"""{"error": "Unauthenticated", "messageId": "${message.id.value}"}"""

    /** Format forbidden error as JSON.
      *
      * Includes resource type and action information to help clients understand what permission was
      * required. The resource identifier is sanitized to only include the namespace (type), not the
      * specific ID, to prevent information disclosure about resource existence and ID formats.
      *
      * @param resource
      *   The resource identifier (format: "namespace:id")
      * @param action
      *   The attempted action
      * @return
      *   JSON string with error, resource type, and action fields
      */
    private def formatForbiddenError(resource: String, action: String): String =
        val resourceType = resource.split(":").headOption.getOrElse("resource")
        s"""{"error": "Forbidden", "resourceType": "$resourceType", "action": "$action"}"""
    end formatForbiddenError

    /** Log authentication failure for security monitoring.
      *
      * Uses info level logging for visibility without triggering alerts. Authentication failures
      * are expected in normal operation and should be monitored but not treated as errors.
      *
      * @param errorType
      *   Type of authentication error
      * @param details
      *   Additional context
      */
    private def logAuthFailure(errorType: String, details: String): Unit =
        logger.info(s"Authentication failure: type=$errorType details=$details")

end AuthErrorHandler
