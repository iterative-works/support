// PURPOSE: Test suite for AlwaysAllowPermissionService
// PURPOSE: Verifies always-grant behavior for testing environments

package works.iterative.core.auth

import zio.*
import zio.test.*
import zio.test.Assertion.*

object AlwaysAllowPermissionServiceSpec extends ZIOSpecDefault:
  case class TestUser(subjectId: UserId) extends UserInfo

  def spec = suite("AlwaysAllowPermissionService")(
    test("isAllowed always returns true for any user and resource"):
      for
        service <- ZIO.service[AlwaysAllowPermissionService]
        user1 = TestUser(UserId.unsafe("user1"))
        user2 = TestUser(UserId.unsafe("user2"))
        target1 = PermissionTarget.unsafe("document", "123")
        target2 = PermissionTarget.unsafe("file", "456")
        result1 <- service.isAllowed(user1, PermissionOp.unsafe("read"), target1)
        result2 <- service.isAllowed(user2, PermissionOp.unsafe("write"), target2)
        result3 <- service.isAllowed(Some(user1), PermissionOp.unsafe("delete"), target1)
        result4 <- service.isAllowed(None, PermissionOp.unsafe("read"), target2)
      yield assertTrue(result1 && result2 && result3 && result4)
    ,
    test("listAllowed returns empty set since we can't enumerate all possible resources"):
      for
        service <- ZIO.service[AlwaysAllowPermissionService]
        user = TestUser(UserId.unsafe("user1"))
        result <- service.listAllowed(user, PermissionOp.unsafe("read"), "document")
      yield assertTrue(result.isEmpty)
  ).provide(AlwaysAllowPermissionService.layer)
