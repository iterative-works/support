// PURPOSE: Tests for audit logging service
// PURPOSE: Validates audit event recording for permission checks and authentication events

package works.iterative.core.audit

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.{UserId, PermissionTarget, PermissionOp}
import java.time.Instant

object AuditLogServiceSpec extends ZIOSpecDefault:

  def spec = suite("AuditLogServiceSpec")(
    test("logPermissionCheck records permission check event") {
      for {
        service <- ZIO.service[AuditLogService]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "123")
        action = PermissionOp.unsafe("read")
        _ <- service.logPermissionCheck(userId, target, action, result = true)
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
      } yield assertTrue(
        events.size == 1,
        events.head.userId.contains(userId),
        events.head.resource.contains("document:123"),
        events.head.action.contains("read"),
        events.head.result == "allowed",
        events.head.eventType == "permission_check"
      )
    },

    test("logPermissionCheck records denied permission") {
      for {
        service <- ZIO.service[AuditLogService]
        userId = UserId.unsafe("user2")
        target = PermissionTarget.unsafe("file", "456")
        action = PermissionOp.unsafe("write")
        _ <- service.logPermissionCheck(userId, target, action, result = false, Some("No relation"))
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
      } yield assertTrue(
        events.size == 1,
        events.head.result == "denied",
        events.head.reason.contains("No relation")
      )
    },

    test("logAuthenticationEvent records login event") {
      for {
        service <- ZIO.service[AuditLogService]
        userId = UserId.unsafe("user3")
        _ <- service.logAuthenticationEvent(
          Some(userId),
          "login",
          success = true,
          Map("ip" -> "192.168.1.1", "userAgent" -> "Mozilla/5.0")
        )
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
      } yield assertTrue(
        events.size == 1,
        events.head.eventType == "login",
        events.head.result == "success",
        events.head.userId.contains(userId),
        events.head.metadata.get("ip").contains("192.168.1.1")
      )
    },

    test("logAuthenticationEvent records failed login") {
      for {
        service <- ZIO.service[AuditLogService]
        _ <- service.logAuthenticationEvent(
          None,
          "login_failed",
          success = false,
          Map("username" -> "unknown", "reason" -> "invalid_credentials")
        )
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
      } yield assertTrue(
        events.size == 1,
        events.head.eventType == "login_failed",
        events.head.result == "failure",
        events.head.userId.isEmpty,
        events.head.metadata.get("reason").contains("invalid_credentials")
      )
    },

    test("audit events have timestamps") {
      for {
        service <- ZIO.service[AuditLogService]
        before <- Clock.instant
        userId = UserId.unsafe("user4")
        target = PermissionTarget.unsafe("document", "789")
        action = PermissionOp.unsafe("delete")
        _ <- service.logPermissionCheck(userId, target, action, result = true)
        after <- Clock.instant
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
      } yield assertTrue(
        events.size == 1,
        !events.head.timestamp.isBefore(before),
        !events.head.timestamp.isAfter(after)
      )
    },

    test("formatAsJson produces valid JSON string") {
      for {
        service <- ZIO.service[AuditLogService]
        userId = UserId.unsafe("user5")
        target = PermissionTarget.unsafe("document", "123")
        action = PermissionOp.unsafe("read")
        _ <- service.logPermissionCheck(userId, target, action, result = true)
        events <- service match
          case impl: InMemoryAuditLogService => impl.getEvents
          case _ => ZIO.succeed(List.empty)
        json = events.head.formatAsJson
      } yield assertTrue(
        json.contains("\"userId\""),
        json.contains("\"eventType\""),
        json.contains("\"resource\""),
        json.contains("\"action\""),
        json.contains("\"result\""),
        json.contains("\"timestamp\"")
      )
    }
  ).provide(
    InMemoryAuditLogService.layer
  )

end AuditLogServiceSpec
