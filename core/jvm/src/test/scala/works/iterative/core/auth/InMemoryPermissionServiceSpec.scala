package works.iterative.core.auth

import zio.*
import zio.test.*
import zio.test.Assertion.*

object InMemoryPermissionServiceSpec extends ZIOSpecDefault:

  // Create a simple UserInfo implementation for testing
  case class TestUser(subjectId: UserId) extends UserInfo

  def spec = suite("InMemoryPermissionServiceSpec")(
    test("direct permission: user has 'owner' relation, check 'owner' permission returns true") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "123")
        _ <- service match
          case impl: InMemoryPermissionService =>
            impl.addRelation(userId, "owner", target)
          case _ => ZIO.unit
        user = TestUser(userId)
        result <- service.isAllowed(user, PermissionOp.unsafe("owner"), target)
      } yield assertTrue(result)
    },

    test("computed permission: user has 'owner' relation, check 'view' permission returns true via inheritance") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "123")
        _ <- service match
          case impl: InMemoryPermissionService =>
            impl.addRelation(userId, "owner", target)
          case _ => ZIO.unit
        user = TestUser(userId)
        // Owner should imply view permission through config
        result <- service.isAllowed(user, PermissionOp.unsafe("view"), target)
      } yield assertTrue(result)
    },

    test("denied permission: user lacks any relation returns false") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "123")
        user = TestUser(userId)
        result <- service.isAllowed(user, PermissionOp.unsafe("view"), target)
      } yield assertTrue(!result)
    },

    test("addRelation and removeRelation operations") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "123")
        impl <- ZIO.succeed(service.asInstanceOf[InMemoryPermissionService])
        _ <- impl.addRelation(userId, "owner", target)
        result1 <- impl.isAllowed(TestUser(userId), PermissionOp.unsafe("owner"), target)
        _ <- impl.removeRelation(userId, "owner", target)
        result2 <- impl.isAllowed(TestUser(userId), PermissionOp.unsafe("owner"), target)
      } yield assertTrue(result1 && !result2)
    },

    test("listAllowed returns all resources user can access") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target1 = PermissionTarget.unsafe("document", "123")
        target2 = PermissionTarget.unsafe("document", "456")
        target3 = PermissionTarget.unsafe("document", "789")
        impl <- ZIO.succeed(service.asInstanceOf[InMemoryPermissionService])
        _ <- impl.addRelation(userId, "owner", target1)
        _ <- impl.addRelation(userId, "editor", target2)
        // target3 has no relation
        allowed <- impl.listAllowed(TestUser(userId), PermissionOp.unsafe("view"), "document")
      } yield assertTrue(allowed.contains("123") && allowed.contains("456") && !allowed.contains("789"))
    },

    test("listAllowed interface method explicitly tested") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId = UserId.unsafe("user1")
        target1 = PermissionTarget.unsafe("document", "doc1")
        target2 = PermissionTarget.unsafe("document", "doc2")
        target3 = PermissionTarget.unsafe("folder", "folder1")
        impl <- ZIO.succeed(service.asInstanceOf[InMemoryPermissionService])
        _ <- impl.addRelation(userId, "owner", target1)
        _ <- impl.addRelation(userId, "editor", target2)
        _ <- impl.addRelation(userId, "owner", target3)
        // Call through PermissionService interface
        allowed <- service.listAllowed(TestUser(userId), PermissionOp.unsafe("view"), "document")
      } yield assertTrue(
        allowed.contains("doc1") &&
        allowed.contains("doc2") &&
        !allowed.contains("folder1")
      )
    },

    test("property-based: listAllowed returns correct subset with 100 random relation tuples") {
      for {
        service <- ZIO.service[InMemoryPermissionService]
        userId1 = UserId.unsafe("user1")
        userId2 = UserId.unsafe("user2")
        impl <- ZIO.succeed(service.asInstanceOf[InMemoryPermissionService])
        // Add 100 random tuples
        _ <- ZIO.foreachDiscard(1 to 100) { i =>
          val user = if i % 2 == 0 then userId1 else userId2
          val relation = if i % 3 == 0 then "owner" else "editor"
          val target = PermissionTarget.unsafe("document", s"doc$i")
          impl.addRelation(user, relation, target)
        }
        // user1 should have access to documents where i is even (50 documents)
        // Call through PermissionService interface
        allowed <- service.listAllowed(TestUser(userId1), PermissionOp.unsafe("view"), "document")
      } yield assertTrue(
        allowed.size == 50 &&
        allowed.contains("doc2") &&
        allowed.contains("doc100") &&
        !allowed.contains("doc1") &&
        !allowed.contains("doc99")
      )
    }
  ).provide(
    ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default))
  )
end InMemoryPermissionServiceSpec
