// PURPOSE: Integration tests for PostgreSQL PermissionRepository implementation
// PURPOSE: Verifies repository operations using real PostgreSQL database with Testcontainers

package works.iterative.sqldb.postgresql

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.auth.*
import works.iterative.sqldb.{FlywayMigrationService, PermissionRepository}
import works.iterative.sqldb.postgresql.testing.PostgreSQLTestingLayers.*

object PostgreSQLPermissionRepositorySpec extends ZIOSpecDefault:

  def spec = suite("PostgreSQLPermissionRepositorySpec")(
    test("hasRelation returns true when relation exists in database") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user1")
        target = PermissionTarget.unsafe("document", "doc1")

        // Add relation
        _ <- repository.addRelation(userId, "owner", target)

        // Check it exists
        exists <- repository.hasRelation(userId, "owner", target)
      yield assertTrue(exists)
    },

    test("hasRelation returns false when relation does not exist in database") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user2")
        target = PermissionTarget.unsafe("document", "doc2")

        // Check non-existent relation
        exists <- repository.hasRelation(userId, "viewer", target)
      yield assertTrue(!exists)
    },

    test("addRelation persists relation tuple across connections") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user3")
        target = PermissionTarget.unsafe("project", "proj1")

        // Add relation
        _ <- repository.addRelation(userId, "editor", target)

        // Verify persistence (hasRelation uses separate query)
        exists <- repository.hasRelation(userId, "editor", target)
      yield assertTrue(exists)
    },

    test("addRelation is idempotent (duplicate insert succeeds)") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user4")
        target = PermissionTarget.unsafe("document", "doc4")

        // Add same relation twice
        _ <- repository.addRelation(userId, "owner", target)
        result <- repository.addRelation(userId, "owner", target).either

        // Second insert should succeed (idempotent)
        exists <- repository.hasRelation(userId, "owner", target)
      yield assertTrue(result.isRight && exists)
    },

    test("removeRelation deletes tuple from database") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user5")
        target = PermissionTarget.unsafe("file", "file1")

        // Add and then remove
        _ <- repository.addRelation(userId, "viewer", target)
        existsBefore <- repository.hasRelation(userId, "viewer", target)

        _ <- repository.removeRelation(userId, "viewer", target)
        existsAfter <- repository.hasRelation(userId, "viewer", target)
      yield assertTrue(existsBefore && !existsAfter)
    },

    test("removeRelation is idempotent (removing non-existent succeeds)") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user6")
        target = PermissionTarget.unsafe("document", "doc6")

        // Remove non-existent relation
        result <- repository.removeRelation(userId, "editor", target).either
      yield assertTrue(result.isRight)
    },

    test("getUserRelations returns all relations for user in namespace") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user7")
        doc1 = PermissionTarget.unsafe("document", "doc1")
        doc2 = PermissionTarget.unsafe("document", "doc2")
        doc3 = PermissionTarget.unsafe("document", "doc3")
        proj1 = PermissionTarget.unsafe("project", "proj1")

        // Add multiple relations
        _ <- repository.addRelation(userId, "owner", doc1)
        _ <- repository.addRelation(userId, "editor", doc2)
        _ <- repository.addRelation(userId, "viewer", doc3)
        _ <- repository.addRelation(userId, "owner", proj1)

        // Get only document relations
        docRelations <- repository.getUserRelations(userId, "document")
      yield assertTrue(
        docRelations.size == 3 &&
        docRelations.contains(RelationTuple(userId, "owner", doc1)) &&
        docRelations.contains(RelationTuple(userId, "editor", doc2)) &&
        docRelations.contains(RelationTuple(userId, "viewer", doc3)) &&
        !docRelations.exists(_.target.namespace == "project")
      )
    },

    test("getUserRelations returns empty set when user has no relations") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user8")

        // No relations added
        relations <- repository.getUserRelations(userId, "document")
      yield assertTrue(relations.isEmpty)
    },

    test("handles special characters in user_id and object_id") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("user@example.com")
        target = PermissionTarget.unsafe("document", "doc-with-special:chars/123")

        // Add relation with special characters
        _ <- repository.addRelation(userId, "owner", target)

        // Verify it works
        exists <- repository.hasRelation(userId, "owner", target)
        relations <- repository.getUserRelations(userId, "document")
      yield assertTrue(
        exists &&
        relations.size == 1 &&
        relations.head.user == userId &&
        relations.head.target.value == "doc-with-special:chars/123"
      )
    },

    test("handles SQL injection attempt safely") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        repository <- ZIO.service[PermissionRepository]

        // Malicious inputs designed to break parameterized queries
        maliciousUserId = UserId.unsafe("user'; DROP TABLE permissions; --")
        maliciousRelation = "owner'; DROP TABLE permissions; --"
        target = PermissionTarget.unsafe("document", "doc1")

        // Add relation with malicious data
        _ <- repository.addRelation(maliciousUserId, maliciousRelation, target)

        // Should safely store the malicious strings as data
        exists <- repository.hasRelation(maliciousUserId, maliciousRelation, target)

        // Verify table still exists by querying it
        relations <- repository.getUserRelations(maliciousUserId, "document")
      yield assertTrue(
        exists &&
        relations.size == 1 &&
        relations.head.relation == maliciousRelation
      )
    }
  ).provideSomeShared[Scope](
    PostgreSQLPermissionRepository.layer,
    flywayMigrationServiceLayer
  ) @@ sequential

end PostgreSQLPermissionRepositorySpec
