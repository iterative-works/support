// PURPOSE: End-to-end integration tests for DatabasePermissionService
// PURPOSE: Verifies complete permission workflow with real database persistence and inheritance logic

package works.iterative.sqldb.postgresql

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.core.auth.*
import works.iterative.sqldb.{FlywayMigrationService, PermissionRepository, DatabasePermissionService}
import works.iterative.sqldb.postgresql.testing.PostgreSQLTestingLayers.*

object DatabasePermissionServiceE2ESpec extends ZIOSpecDefault:

  // Simple test implementation of UserInfo
  case class TestUser(subjectId: UserId) extends UserInfo

  // Test configuration with inheritance rules
  // Relations are: owner, editor, viewer
  // Actions are: view, edit, delete
  // owner relation implies edit and view actions
  // editor relation implies edit and view actions
  // viewer relation implies view action
  val testConfig = PermissionConfig(
    namespaces = Map(
      "document" -> NamespaceConfig(
        implications = Map(
          "owner" -> Set("edit", "view", "delete"),
          "editor" -> Set("edit", "view"),
          "viewer" -> Set("view")
        )
      ),
      "project" -> NamespaceConfig(
        implications = Map(
          "admin" -> Set("manage", "view"),
          "member" -> Set("view")
        )
      )
    )
  )

  def spec = suite("DatabasePermissionServiceE2ESpec")(
    test("complete flow: grant → check allowed → revoke → check denied") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("alice")
        userInfo = TestUser(userId)
        target = PermissionTarget.unsafe("document", "doc1")
        viewOp = PermissionOp.unsafe("view")

        // Initially, user has no permission
        allowedBefore <- permissionService.isAllowed(Some(userInfo), viewOp, target)

        // Grant viewer permission
        _ <- permissionService.grantPermission(userId, "viewer", target)

        // Now user should have permission
        allowedAfter <- permissionService.isAllowed(Some(userInfo), viewOp, target)

        // Revoke permission
        _ <- permissionService.revokePermission(userId, "viewer", target)

        // Permission should be denied again
        allowedFinal <- permissionService.isAllowed(Some(userInfo), viewOp, target)
      yield assertTrue(
        !allowedBefore &&
        allowedAfter &&
        !allowedFinal
      )
    },

    test("permission inheritance works with database storage (owner implies editor implies viewer)") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("bob")
        userInfo = TestUser(userId)
        target = PermissionTarget.unsafe("document", "doc2")
        viewOp = PermissionOp.unsafe("view")
        editOp = PermissionOp.unsafe("edit")

        // Grant owner permission (should imply editor and viewer)
        _ <- permissionService.grantPermission(userId, "owner", target)

        // Check that owner can view
        canView <- permissionService.isAllowed(Some(userInfo), viewOp, target)

        // Check that owner can edit
        canEdit <- permissionService.isAllowed(Some(userInfo), editOp, target)
      yield assertTrue(
        canView &&
        canEdit
      )
    },

    test("listAllowed returns correct resources with inheritance") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("charlie")
        userInfo = TestUser(userId)
        doc1 = PermissionTarget.unsafe("document", "doc1")
        doc2 = PermissionTarget.unsafe("document", "doc2")
        doc3 = PermissionTarget.unsafe("document", "doc3")
        viewOp = PermissionOp.unsafe("view")

        // Grant different permission levels
        _ <- permissionService.grantPermission(userId, "owner", doc1)   // can view
        _ <- permissionService.grantPermission(userId, "editor", doc2)  // can view
        _ <- permissionService.grantPermission(userId, "viewer", doc3)  // can view

        // List all documents user can view
        allowedDocs <- permissionService.listAllowed(userInfo, viewOp, "document")
      yield assertTrue(
        allowedDocs.size == 3 &&
        allowedDocs.contains("doc1") &&
        allowedDocs.contains("doc2") &&
        allowedDocs.contains("doc3")
      )
    },

    test("listAllowed respects permission boundaries") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("diana")
        userInfo = TestUser(userId)
        doc1 = PermissionTarget.unsafe("document", "doc1")
        doc2 = PermissionTarget.unsafe("document", "doc2")
        editOp = PermissionOp.unsafe("edit")

        // Grant owner on doc1 (can edit) but only viewer on doc2 (cannot edit)
        _ <- permissionService.grantPermission(userId, "owner", doc1)
        _ <- permissionService.grantPermission(userId, "viewer", doc2)

        // List documents user can edit
        editable <- permissionService.listAllowed(userInfo, editOp, "document")
      yield assertTrue(
        editable.size == 1 &&
        editable.contains("doc1") &&
        !editable.contains("doc2")
      )
    },

    test("concurrent permission checks are thread-safe") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("eve")
        userInfo = TestUser(userId)
        targets = (1 to 10).map(i => PermissionTarget.unsafe("document", s"doc$i"))
        viewOp = PermissionOp.unsafe("view")

        // Grant permissions concurrently
        _ <- ZIO.foreachPar(targets)(target =>
          permissionService.grantPermission(userId, "viewer", target)
        )

        // Check permissions concurrently
        results <- ZIO.foreachPar(targets)(target =>
          permissionService.isAllowed(Some(userInfo), viewOp, target)
        )
      yield assertTrue(
        results.size == 10 &&
        results.forall(_ == true)  // All should be true
      )
    },

    test("unauthenticated users are denied access") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        target = PermissionTarget.unsafe("document", "doc1")
        viewOp = PermissionOp.unsafe("view")

        // Grant permission to a user, but check with None
        _ <- permissionService.grantPermission(UserId.unsafe("alice"), "viewer", target)

        // Unauthenticated check should be denied
        allowed <- permissionService.isAllowed(None, viewOp, target)
      yield assertTrue(!allowed)
    },

    test("fail-closed: database errors result in access denial") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]
        repository <- ZIO.service[PermissionRepository]

        userId = UserId.unsafe("frank")
        userInfo = TestUser(userId)
        target = PermissionTarget.unsafe("document", "doc1")
        viewOp = PermissionOp.unsafe("view")

        // Grant permission
        _ <- permissionService.grantPermission(userId, "viewer", target)

        // Verify it works
        allowed <- permissionService.isAllowed(Some(userInfo), viewOp, target)

        // Clean the database (simulating database failure by removing the schema)
        _ <- migrationService.clean()

        // Now checks should fail closed (return false, not throw exception)
        deniedAfterError <- permissionService.isAllowed(Some(userInfo), viewOp, target)
      yield assertTrue(
        allowed &&
        !deniedAfterError
      )
    },

    test("multiple users can have different permissions on same resource") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        alice = TestUser(UserId.unsafe("alice"))
        bob = TestUser(UserId.unsafe("bob"))
        charlie = TestUser(UserId.unsafe("charlie"))
        target = PermissionTarget.unsafe("document", "doc1")
        viewOp = PermissionOp.unsafe("view")
        editOp = PermissionOp.unsafe("edit")

        // Grant different permissions
        _ <- permissionService.grantPermission(UserId.unsafe("alice"), "owner", target)
        _ <- permissionService.grantPermission(UserId.unsafe("bob"), "editor", target)
        _ <- permissionService.grantPermission(UserId.unsafe("charlie"), "viewer", target)

        // Check permissions
        aliceCanEdit <- permissionService.isAllowed(Some(alice), editOp, target)
        bobCanEdit <- permissionService.isAllowed(Some(bob), editOp, target)
        charlieCanEdit <- permissionService.isAllowed(Some(charlie), editOp, target)
        charlieCanView <- permissionService.isAllowed(Some(charlie), viewOp, target)
      yield assertTrue(
        aliceCanEdit &&
        bobCanEdit &&
        !charlieCanEdit &&
        charlieCanView
      )
    },

    test("permissions are namespace-isolated") {
      for
        migrationService <- ZIO.service[FlywayMigrationService]
        _ <- migrationService.clean()
        _ <- migrationService.migrate()
        permissionService <- ZIO.service[DatabasePermissionService]

        userId = UserId.unsafe("george")
        userInfo = TestUser(userId)
        docTarget = PermissionTarget.unsafe("document", "item1")
        projTarget = PermissionTarget.unsafe("project", "item1")
        viewOp = PermissionOp.unsafe("view")

        // Grant permission in document namespace
        _ <- permissionService.grantPermission(userId, "viewer", docTarget)

        // Check permission in document namespace (should succeed)
        canViewDoc <- permissionService.isAllowed(Some(userInfo), viewOp, docTarget)

        // Check permission in project namespace (should fail - different namespace)
        canViewProj <- permissionService.isAllowed(Some(userInfo), viewOp, projTarget)
      yield assertTrue(
        canViewDoc &&
        !canViewProj
      )
    }
  ).provideSomeShared[Scope](
    // Provide config
    ZLayer.succeed(testConfig),
    // Provide repository
    PostgreSQLPermissionRepository.layer,
    // Provide DatabasePermissionService
    DatabasePermissionService.layer,
    // Provide testing infrastructure
    flywayMigrationServiceLayer
  ) @@ sequential

end DatabasePermissionServiceE2ESpec
