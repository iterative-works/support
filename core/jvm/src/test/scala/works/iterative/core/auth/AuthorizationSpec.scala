// PURPOSE: Test suite for Authorization helper object providing declarative permission guards
// PURPOSE: Validates require, check, withPermission, and filterAllowed methods with various scenarios

package works.iterative.core.auth

import zio.*
import zio.test.*

import works.iterative.core.{UserName, Email}

object AuthorizationSpec extends ZIOSpecDefault:

  // Test user for authorization checks
  val testUserId = UserId.unsafe("test-user")
  val testUser: BasicProfile = BasicProfile(
    subjectId = testUserId,
    userName = Some(UserName.unsafe("Test User")),
    email = Some(Email.unsafe("test@example.com")),
    avatar = None,
    roles = Set.empty
  )

  // Create CurrentUser layer for tests
  val currentUserLayer: ULayer[CurrentUser] = ZLayer.succeed(CurrentUser(testUser))

  def spec = suite("AuthorizationSpec")(
    test("require allows effect when permission granted") {
      for {
        service <- ZIO.service[PermissionService]
        target = PermissionTarget.unsafe("document", "123")
        op = PermissionOp.unsafe("view")

        // Grant permission
        impl = service.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(testUserId, "viewer", target)

        // Should execute effect
        result <- Authorization.require(op, target)(ZIO.succeed("success"))
      } yield assertTrue(result == "success")
    },

    test("require fails with Forbidden when permission denied") {
      val target = PermissionTarget.unsafe("document", "456")
      val op = PermissionOp.unsafe("edit")

      for {
        // No permission granted

        // Should fail with Forbidden
        result <- Authorization.require(op, target)(ZIO.succeed("success")).exit
      } yield assertTrue(result.isFailure)
    },

    test("check returns Boolean (typed error channel)") {
      for {
        service <- ZIO.service[PermissionService]
        target = PermissionTarget.unsafe("document", "789")
        op = PermissionOp.unsafe("view")

        // Grant permission
        impl = service.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(testUserId, "viewer", target)

        // Should return true
        result <- Authorization.check(op, target)
      } yield assertTrue(result == true)
    },

    test("check returns false when permission denied (no exception)") {
      val target = PermissionTarget.unsafe("document", "999")
      val op = PermissionOp.unsafe("delete")

      for {
        // No permission granted

        // Should return false (not throw)
        result <- Authorization.check(op, target)
      } yield assertTrue(result == false)
    },

    test("withPermission filters effect result when permission denied") {
      val target = PermissionTarget.unsafe("document", "111")
      val op = PermissionOp.unsafe("view")

      for {
        // No permission granted

        // Should return None
        result <- Authorization.withPermission(op, target)(ZIO.succeed(Some("data")))
      } yield assertTrue(result.isEmpty)
    },

    test("withPermission returns effect result when permission granted") {
      for {
        service <- ZIO.service[PermissionService]
        target = PermissionTarget.unsafe("document", "222")
        op = PermissionOp.unsafe("view")

        // Grant permission
        impl = service.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(testUserId, "viewer", target)

        // Should return Some("data")
        result <- Authorization.withPermission(op, target)(ZIO.succeed(Some("data")))
      } yield assertTrue(result.contains("data"))
    },

    test("filterAllowed filters list of resources by permission") {
      case class Document(id: String, name: String)

      for {
        service <- ZIO.service[PermissionService]
        op = PermissionOp.unsafe("view")

        // Grant permission to document 1 and 3, but not 2
        impl = service.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(testUserId, "viewer", PermissionTarget.unsafe("document", "1"))
        _ <- impl.addRelation(testUserId, "viewer", PermissionTarget.unsafe("document", "3"))

        documents = Seq(
          Document("1", "Doc 1"),
          Document("2", "Doc 2"),
          Document("3", "Doc 3")
        )

        // Should return only documents 1 and 3
        allowed <- Authorization.filterAllowed(op, documents)(doc => PermissionTarget.unsafe("document", doc.id))
      } yield assertTrue(
        allowed.size == 2,
        allowed.exists(_.id == "1"),
        allowed.exists(_.id == "3"),
        !allowed.exists(_.id == "2")
      )
    }
  ).provide(
    currentUserLayer,
    ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default))
  )
