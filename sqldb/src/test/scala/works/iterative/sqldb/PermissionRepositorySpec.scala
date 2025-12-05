// PURPOSE: Unit tests for PermissionRepository trait interface
// PURPOSE: Verifies repository contract using in-memory test implementation

package works.iterative.sqldb

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.auth.*

object PermissionRepositorySpec extends ZIOSpecDefault:

  // In-memory test implementation for validating the repository contract
  class InMemoryPermissionRepository extends PermissionRepository:
    private val storage = scala.collection.mutable.Set.empty[RelationTuple]

    override def hasRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Boolean] =
      ZIO.succeed(storage.contains(RelationTuple(userId, relation, target)))

    override def addRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
      ZIO.succeed(storage.add(RelationTuple(userId, relation, target))).unit

    override def removeRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
      ZIO.succeed(storage.remove(RelationTuple(userId, relation, target))).unit

    override def getUserRelations(
        userId: UserId,
        namespace: String
    ): Task[Set[RelationTuple]] =
      ZIO.succeed(
        storage.filter(t => t.user == userId && t.target.namespace == namespace).toSet
      )
  end InMemoryPermissionRepository

  val testLayer: ULayer[PermissionRepository] =
    ZLayer.succeed(InMemoryPermissionRepository())

  def spec = suite("PermissionRepositorySpec")(
    test("hasRelation returns true when relation exists") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "doc1")
        _ <- repo.addRelation(userId, "owner", target)
        exists <- repo.hasRelation(userId, "owner", target)
      yield assertTrue(exists)
    },

    test("hasRelation returns false when relation does not exist") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "doc1")
        exists <- repo.hasRelation(userId, "owner", target)
      yield assertTrue(!exists)
    },

    test("addRelation followed by hasRelation returns true") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user2")
        target = PermissionTarget.unsafe("project", "proj1")
        existsBefore <- repo.hasRelation(userId, "editor", target)
        _ <- repo.addRelation(userId, "editor", target)
        existsAfter <- repo.hasRelation(userId, "editor", target)
      yield assertTrue(!existsBefore && existsAfter)
    },

    test("removeRelation followed by hasRelation returns false") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user3")
        target = PermissionTarget.unsafe("file", "file1")
        _ <- repo.addRelation(userId, "viewer", target)
        existsBefore <- repo.hasRelation(userId, "viewer", target)
        _ <- repo.removeRelation(userId, "viewer", target)
        existsAfter <- repo.hasRelation(userId, "viewer", target)
      yield assertTrue(existsBefore && !existsAfter)
    },

    test("getUserRelations returns all relations for user in namespace") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user4")
        doc1 = PermissionTarget.unsafe("document", "doc1")
        doc2 = PermissionTarget.unsafe("document", "doc2")
        proj1 = PermissionTarget.unsafe("project", "proj1")
        _ <- repo.addRelation(userId, "owner", doc1)
        _ <- repo.addRelation(userId, "editor", doc2)
        _ <- repo.addRelation(userId, "owner", proj1)
        docRelations <- repo.getUserRelations(userId, "document")
      yield assertTrue(
        docRelations.size == 2 &&
        docRelations.contains(RelationTuple(userId, "owner", doc1)) &&
        docRelations.contains(RelationTuple(userId, "editor", doc2)) &&
        !docRelations.exists(_.target.namespace == "project")
      )
    },

    test("getUserRelations returns empty set when user has no relations in namespace") {
      for
        repo <- ZIO.service[PermissionRepository]
        userId = UserId.unsafe("user5")
        relations <- repo.getUserRelations(userId, "document")
      yield assertTrue(relations.isEmpty)
    }
  ).provide(testLayer) @@ sequential

end PermissionRepositorySpec
