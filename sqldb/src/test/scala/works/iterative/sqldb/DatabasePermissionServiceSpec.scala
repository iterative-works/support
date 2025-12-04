// PURPOSE: Unit tests for DatabasePermissionService fail-closed behavior
// PURPOSE: Verifies service correctly delegates to repository and handles errors safely

package works.iterative.sqldb

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.auth.*

object DatabasePermissionServiceSpec extends ZIOSpecDefault:

  // Mock repository that can simulate failures
  class TestPermissionRepository(
      storage: Ref[Set[RelationTuple]],
      shouldFail: Ref[Boolean]
  ) extends PermissionRepository:

    override def hasRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Boolean] =
      shouldFail.get.flatMap {
        case true => ZIO.fail(new Exception("Database connection failed"))
        case false => storage.get.map(_.contains(RelationTuple(userId, relation, target)))
      }

    override def addRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
      shouldFail.get.flatMap {
        case true => ZIO.fail(new Exception("Database connection failed"))
        case false => storage.update(_ + RelationTuple(userId, relation, target))
      }

    override def removeRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
      shouldFail.get.flatMap {
        case true => ZIO.fail(new Exception("Database connection failed"))
        case false => storage.update(_ - RelationTuple(userId, relation, target))
      }

    override def getUserRelations(
        userId: UserId,
        namespace: String
    ): Task[Set[RelationTuple]] =
      shouldFail.get.flatMap {
        case true => ZIO.fail(new Exception("Database connection failed"))
        case false =>
          storage.get.map(_.filter(t => t.user == userId && t.target.namespace == namespace))
      }
  end TestPermissionRepository

  def makeTestRepository: UIO[(PermissionRepository, Ref[Boolean])] =
    for
      storage <- Ref.make(Set.empty[RelationTuple])
      shouldFail <- Ref.make(false)
    yield (TestPermissionRepository(storage, shouldFail), shouldFail)

  val testConfig: PermissionConfig = PermissionConfig(
    namespaces = Map(
      "document" -> NamespaceConfig(
        implications = Map(
          "owner" -> Set("view", "edit", "delete"),
          "editor" -> Set("view", "edit"),
          "viewer" -> Set("view")
        )
      )
    )
  )

  // Simple test implementation of UserInfo
  case class TestUser(subjectId: UserId, email: String) extends UserInfo

  def spec = suite("DatabasePermissionServiceSpec")(
    test("isAllowed uses stored permissions from repository") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "doc1")
        user = TestUser(userId, "user1@example.com")

        // Initially not allowed
        notAllowedBefore <- service.isAllowed(user, PermissionOp.unsafe("view"), target)

        // Grant permission
        _ <- service.grantPermission(userId, "viewer", target)

        // Now allowed
        allowedAfter <- service.isAllowed(user, PermissionOp.unsafe("view"), target)
      yield assertTrue(!notAllowedBefore && allowedAfter)
    },

    test("isAllowed with permission inheritance (owner implies editor)") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user2")
        target = PermissionTarget.unsafe("document", "doc2")
        user = TestUser(userId, "user2@example.com")

        // Grant owner permission
        _ <- service.grantPermission(userId, "owner", target)

        // Check that owner implies view, edit, and delete
        canView <- service.isAllowed(user, PermissionOp.unsafe("view"), target)
        canEdit <- service.isAllowed(user, PermissionOp.unsafe("edit"), target)
        canDelete <- service.isAllowed(user, PermissionOp.unsafe("delete"), target)
      yield assertTrue(canView && canEdit && canDelete)
    },

    test("listAllowed returns correct resources") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user3")
        doc1 = PermissionTarget.unsafe("document", "doc1")
        doc2 = PermissionTarget.unsafe("document", "doc2")
        doc3 = PermissionTarget.unsafe("document", "doc3")
        user = TestUser(userId, "user3@example.com")

        // Grant different permissions
        _ <- service.grantPermission(userId, "owner", doc1)
        _ <- service.grantPermission(userId, "editor", doc2)
        // doc3 - no permission

        // List documents user can edit
        editableDocs <- service.listAllowed(user, PermissionOp.unsafe("edit"), "document")

        // List documents user can view
        viewableDocs <- service.listAllowed(user, PermissionOp.unsafe("view"), "document")
      yield assertTrue(
        editableDocs == Set("doc1", "doc2") &&  // owner and editor can edit
        viewableDocs == Set("doc1", "doc2")     // owner and editor can view
      )
    },

    test("grantPermission persists to repository") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user4")
        target = PermissionTarget.unsafe("document", "doc4")

        _ <- service.grantPermission(userId, "editor", target)

        // Verify it was persisted
        exists <- repo.hasRelation(userId, "editor", target)
      yield assertTrue(exists)
    },

    test("revokePermission removes from repository") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user5")
        target = PermissionTarget.unsafe("document", "doc5")

        _ <- service.grantPermission(userId, "viewer", target)
        existsBefore <- repo.hasRelation(userId, "viewer", target)

        _ <- service.revokePermission(userId, "viewer", target)
        existsAfter <- repo.hasRelation(userId, "viewer", target)
      yield assertTrue(existsBefore && !existsAfter)
    },

    test("isAllowed returns false on database error (fail-closed)") {
      for
        (repo, shouldFail) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        userId = UserId.unsafe("user6")
        target = PermissionTarget.unsafe("document", "doc6")
        user = TestUser(userId, "user6@example.com")

        // Grant permission first (while database works)
        _ <- service.grantPermission(userId, "owner", target)

        // Verify it works normally
        allowedBefore <- service.isAllowed(user, PermissionOp.unsafe("view"), target)

        // Simulate database failure
        _ <- shouldFail.set(true)

        // Should return false (fail-closed) despite permission existing
        allowedAfter <- service.isAllowed(user, PermissionOp.unsafe("view"), target)
      yield assertTrue(allowedBefore && !allowedAfter)
    },

    test("isAllowed returns false for None user") {
      for
        (repo, _) <- makeTestRepository
        service = DatabasePermissionService(repo, testConfig)
        target = PermissionTarget.unsafe("document", "doc7")

        allowed <- service.isAllowed(None, PermissionOp.unsafe("view"), target)
      yield assertTrue(!allowed)
    }
  ) @@ sequential

end DatabasePermissionServiceSpec
