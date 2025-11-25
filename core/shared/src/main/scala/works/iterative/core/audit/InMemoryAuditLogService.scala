// PURPOSE: In-memory audit log service for testing and development
// PURPOSE: Stores audit events in memory for verification in tests

package works.iterative.core.audit

import zio.*
import works.iterative.core.auth.{UserId, PermissionTarget, PermissionOp}

/** In-memory implementation of AuditLogService for testing.
  *
  * This implementation stores audit events in a ZIO Ref for verification
  * in tests. Events are kept in memory and lost when the application restarts.
  *
  * WARNING: Not suitable for production use:
  * - Events are lost on restart
  * - No tamper-evidence
  * - No compliance guarantees
  * - Memory grows unbounded
  *
  * Use this for:
  * - Unit tests
  * - Integration tests
  * - Development environments
  *
  * For production, implement a persistent audit log service that writes to:
  * - Separate audit log stream
  * - Tamper-evident storage (append-only, signed)
  * - Compliant retention and access controls
  *
  * @param events Ref holding the list of audit events
  */
class InMemoryAuditLogService(events: Ref[List[AuditEvent]]) extends AuditLogService:

  def logPermissionCheck(
      userId: UserId,
      resource: PermissionTarget,
      action: PermissionOp,
      result: Boolean,
      reason: Option[String] = None
  ): UIO[Unit] =
    for {
      timestamp <- Clock.instant
      event = AuditEvent(
        timestamp = timestamp,
        userId = Some(userId),
        eventType = "permission_check",
        resource = Some(resource.toString),
        action = Some(action.value),
        result = if result then "allowed" else "denied",
        reason = reason,
        metadata = Map.empty
      )
      _ <- events.update(event :: _)
    } yield ()

  def logAuthenticationEvent(
      userId: Option[UserId],
      eventType: String,
      success: Boolean,
      metadata: Map[String, String] = Map.empty
  ): UIO[Unit] =
    for {
      timestamp <- Clock.instant
      event = AuditEvent(
        timestamp = timestamp,
        userId = userId,
        eventType = eventType,
        resource = None,
        action = None,
        result = if success then "success" else "failure",
        reason = None,
        metadata = metadata
      )
      _ <- events.update(event :: _)
    } yield ()

  /** Get all audit events (for testing).
    *
    * Returns events in reverse chronological order (newest first).
    * This method is only for testing - production implementations
    * should not expose this method.
    */
  def getEvents: UIO[List[AuditEvent]] =
    events.get

end InMemoryAuditLogService

object InMemoryAuditLogService:
  /** Create an InMemoryAuditLogService.
    *
    * @return ZIO effect that creates the service
    */
  def make: UIO[InMemoryAuditLogService] =
    for {
      events <- Ref.make(List.empty[AuditEvent])
    } yield InMemoryAuditLogService(events)

  /** ZLayer factory for InMemoryAuditLogService.
    *
    * Provides AuditLogService.
    */
  val layer: ZLayer[Any, Nothing, AuditLogService] =
    ZLayer.fromZIO(make)

end InMemoryAuditLogService
