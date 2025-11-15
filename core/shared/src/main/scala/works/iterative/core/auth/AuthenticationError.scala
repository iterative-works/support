// PURPOSE: Domain error types for authentication and authorization failures
// PURPOSE: Uses Scala 3 enum for exhaustive pattern matching and type safety

package works.iterative.core.auth

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
    * @param message Description of why authentication failed
    */
  case Unauthenticated(message: String)

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
    * @param reason Description of why token is invalid
    */
  case InvalidToken(reason: String)

end AuthenticationError

object AuthenticationError:
  /** Helper to create an Unauthenticated error for missing token. */
  def missingToken: AuthenticationError = Unauthenticated("No authentication token provided")

  /** Helper to create an Unauthenticated error for missing user. */
  def missingUser: AuthenticationError = Unauthenticated("User not found in context")

  /** Helper to create a Forbidden error for resource access.
    *
    * @param target The permission target
    * @param action The attempted action
    */
  def forbidden(target: PermissionTarget, action: PermissionOp): AuthenticationError =
    Forbidden(target.value, action.value)

  /** Helper to create an InvalidToken error for JWT issues. */
  def invalidJwt(reason: String): AuthenticationError = InvalidToken(s"Invalid JWT: $reason")

end AuthenticationError
