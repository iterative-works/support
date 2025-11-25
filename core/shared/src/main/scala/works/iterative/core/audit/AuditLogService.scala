// PURPOSE: Security audit trail for authentication and authorization events
// PURPOSE: Records permission checks and auth events for compliance and security monitoring

package works.iterative.core.audit

import zio.*
import zio.json.*
import works.iterative.core.auth.{UserId, PermissionTarget, PermissionOp}
import java.time.Instant

/** Audit event representing a security-relevant action.
  *
  * All security events (permission checks, authentication, etc.) are recorded
  * as audit events for compliance, security monitoring, and forensics.
  *
  * @param timestamp When the event occurred
  * @param userId The user involved (None for unauthenticated events)
  * @param eventType Type of event (e.g., "permission_check", "login", "logout")
  * @param resource The resource involved (for permission checks)
  * @param action The action performed (for permission checks)
  * @param result Event result: "allowed", "denied", "success", "failure", "error"
  * @param reason Optional reason for the result
  * @param metadata Additional context (IP address, user agent, error details, etc.)
  */
case class AuditEvent(
    timestamp: Instant,
    userId: Option[UserId],
    eventType: String,
    resource: Option[String],
    action: Option[String],
    result: String,
    reason: Option[String],
    metadata: Map[String, String]
):
  /** Format audit event as JSON string.
    *
    * Uses zio-json for safe JSON encoding to prevent injection attacks.
    *
    * @return JSON representation of the audit event
    */
  def formatAsJson: String =
    import AuditEvent.given
    this.toJson
end AuditEvent

object AuditEvent:
  @annotation.nowarn("msg=Given search preference")
  given JsonEncoder[UserId] = JsonEncoder[String].contramap(_.value)
  given JsonEncoder[Instant] = JsonEncoder[String].contramap(_.toString)
  given JsonEncoder[AuditEvent] = DeriveJsonEncoder.gen[AuditEvent]

/** Service for logging security audit events.
  *
  * All implementations must be fail-safe: logging failures should never
  * interrupt normal application flow. Use UIO (uninterruptible IO) to
  * ensure audit logging cannot cause application failures.
  *
  * Production implementations should:
  * - Write to a separate audit log stream (not application logs)
  * - Ensure tamper-evidence (append-only, cryptographic hashing, etc.)
  * - Meet compliance requirements (retention, encryption, access controls)
  * - Provide alerting for suspicious patterns
  */
trait AuditLogService:

  /** Log a permission check event.
    *
    * Records whether a user was allowed or denied access to a resource.
    * Should be called for EVERY permission check, regardless of result.
    *
    * @param userId The user whose permission was checked
    * @param resource The resource being accessed
    * @param action The action being performed
    * @param result true if allowed, false if denied
    * @param reason Optional reason for denial (e.g., "No relation", "Database error")
    * @return UIO effect that logs the event (never fails)
    */
  def logPermissionCheck(
      userId: UserId,
      resource: PermissionTarget,
      action: PermissionOp,
      result: Boolean,
      reason: Option[String] = None
  ): UIO[Unit]

  /** Log an authentication event.
    *
    * Records login, logout, and authentication failures.
    *
    * @param userId The authenticated user (None for failed authentication)
    * @param eventType Event type: "login", "logout", "login_failed", etc.
    * @param success true if successful, false if failed
    * @param metadata Additional context (IP, user agent, error details, etc.)
    * @return UIO effect that logs the event (never fails)
    */
  def logAuthenticationEvent(
      userId: Option[UserId],
      eventType: String,
      success: Boolean,
      metadata: Map[String, String] = Map.empty
  ): UIO[Unit]

end AuditLogService

object AuditLogService:
  /** Access the audit log service from the ZIO environment. */
  def logPermissionCheck(
      userId: UserId,
      resource: PermissionTarget,
      action: PermissionOp,
      result: Boolean,
      reason: Option[String] = None
  ): URIO[AuditLogService, Unit] =
    ZIO.serviceWithZIO(_.logPermissionCheck(userId, resource, action, result, reason))

  def logAuthenticationEvent(
      userId: Option[UserId],
      eventType: String,
      success: Boolean,
      metadata: Map[String, String] = Map.empty
  ): URIO[AuditLogService, Unit] =
    ZIO.serviceWithZIO(_.logAuthenticationEvent(userId, eventType, success, metadata))
end AuditLogService
