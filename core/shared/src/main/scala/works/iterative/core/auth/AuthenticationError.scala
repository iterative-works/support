// PURPOSE: Domain error types for authentication and authorization failures
// PURPOSE: Uses Scala 3 enum for exhaustive pattern matching and type safety

package works.iterative.core.auth

import works.iterative.core.UserMessage

/** Authentication and authorization error types.
  *
  * This enum defines all possible authentication and authorization failures
  * that can occur in the system. Using Scala 3 enum ensures exhaustive
  * pattern matching and provides type-safe error handling.
  *
  * All variants extend Exception to enable fail-fast error propagation in ZIO.
  */
enum AuthenticationError extends Exception:
  /** User is not authenticated (no valid session or token).
    *
    * Typically maps to HTTP 401 Unauthorized.
    *
    * @param message User-facing message with translation ID
    */
  case Unauthenticated(message: UserMessage)

  /** User is authenticated but lacks permission for the requested operation.
    *
    * Typically maps to HTTP 403 Forbidden.
    *
    * @param resource The resource identifier
    * @param action The attempted action
    */
  case Forbidden(resource: String, action: String)

  /** User provided invalid credentials during login.
    *
    * Typically maps to HTTP 401 Unauthorized.
    */
  case InvalidCredentials

  /** User's authentication token has expired.
    *
    * Typically maps to HTTP 401 Unauthorized with instruction to refresh token.
    */
  case TokenExpired

  /** User's authentication token is invalid or malformed.
    *
    * Typically maps to HTTP 401 Unauthorized.
    *
    * @param message User-facing message with translation ID
    */
  case InvalidToken(message: UserMessage)

end AuthenticationError

object AuthenticationError:
  /** Helper to create an Unauthenticated error for missing token. */
  def missingToken: AuthenticationError =
    Unauthenticated(UserMessage("error.auth.missing_token"))

  /** Helper to create a Forbidden error for resource access.
    *
    * @param target The permission target
    * @param action The attempted action
    */
  def forbidden(target: PermissionTarget, action: PermissionOp): AuthenticationError =
    Forbidden(target.value, action.value)

  /** Helper to create an InvalidToken error for JWT issues.
    *
    * @param reason The reason for the invalid JWT (included in message args)
    */
  def invalidJwt(reason: String): AuthenticationError =
    InvalidToken(UserMessage("error.auth.invalid_token", reason))

end AuthenticationError
