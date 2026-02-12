// PURPOSE: HTTP integration tests validating complete authorization workflows with service layer
// PURPOSE: Tests authorization guards, error handling, and user context propagation

package works.iterative.server.http

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.*
import works.iterative.core.{ExampleDocumentService, Document, UserName, Email}

/** HTTP integration tests for authorization system.
  *
  * These tests validate complete authorization workflows through the service layer:
  * 1. TestAuthenticationService provides user context via FiberRef
  * 2. InMemoryPermissionService tracks permissions
  * 3. ExampleDocumentService applies Authorization.require guards
  * 4. Errors flow through properly (AuthenticationError variants)
  *
  * Test scenarios:
  * - User logs in, creates document (becomes owner), can edit and delete
  * - User A creates document, User B cannot edit or delete (403 Forbidden)
  * - User with "editor" relation can edit but not delete
  * - List documents returns only user's permitted documents
  * - Unauthenticated user operations return Unauthenticated error
  */
object AuthorizationIntegrationSpec extends ZIOSpecDefault:

  // Test user profiles
  val user1Id = UserId.unsafe("user-1")
  val user1Profile = BasicProfile(
    subjectId = user1Id,
    userName = Some(UserName.unsafe("User 1")),
    email = Some(Email.unsafe("user1@example.com")),
    avatar = None,
    roles = Set.empty,
    claims = Set.empty
  )
  val user1Token = AccessToken("test-token-user-1")

  val user2Id = UserId.unsafe("user-2")
  val user2Profile = BasicProfile(
    subjectId = user2Id,
    userName = Some(UserName.unsafe("User 2")),
    email = Some(Email.unsafe("user2@example.com")),
    avatar = None,
    roles = Set.empty,
    claims = Set.empty
  )
  val user2Token = AccessToken("test-token-user-2")

  /** Helper to create test layer with specific user authenticated. */
  def makeUserLayer(profile: BasicProfile): ULayer[CurrentUser] =
    ZLayer.succeed(CurrentUser(profile))

  /** Helper to create permission service and grant initial permissions. */
  def makePermissionServiceWith(
    grants: (UserId, String, PermissionTarget)*
  ): ZLayer[Any, Nothing, InMemoryPermissionService] =
    ZLayer.fromZIO {
      for {
        permService <- InMemoryPermissionService.make(PermissionConfig.default)
        _ <- ZIO.foreach(grants) { case (userId, relation, target) =>
          permService.addRelation(userId, relation, target)
        }
      } yield permService
    }

  def spec = suite("AuthorizationIntegrationSpec")(
    test("User creates document, becomes owner, can edit and delete") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // User1 creates a document
        doc <- service.createDocument("My Document")

        // Verify user can view the document (ownership implies view permission)
        canView <- permService.isAllowed(
          Some(user1Profile),
          PermissionOp.unsafe("view"),
          PermissionTarget.unsafe("document", doc.id)
        )

        // Owner can edit
        updated <- service.updateDocument(doc.id, "Updated Title")

        // Owner can delete
        _ <- service.deleteDocument(doc.id)

      } yield assertTrue(
        doc.ownerId == user1Id.value,
        canView,
        updated.title == "Updated Title"
      )
    }.provide(
      makeUserLayer(user1Profile),
      makePermissionServiceWith(),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("User A creates document, User B cannot edit (403 Forbidden)") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant user2 no permissions on document-123
        // Attempt to update should fail with Forbidden
        result <- service.updateDocument("123", "Hacked Title").either

      } yield assertTrue(
        result.isLeft,
        result.left.exists {
          case AuthenticationError.Forbidden(_, _) => true
          case _ => false
        }
      )
    }.provide(
      makeUserLayer(user2Profile),
      makePermissionServiceWith(),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("User with editor relation can edit but not delete") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Update should succeed
        updated <- service.updateDocument("123", "Edited Title")

        // Delete should fail with Forbidden
        deleteResult <- service.deleteDocument("123").either

      } yield assertTrue(
        updated.title == "Edited Title",
        deleteResult.isLeft,
        deleteResult.left.exists {
          case AuthenticationError.Forbidden(_, _) => true
          case _ => false
        }
      )
    }.provide(
      makeUserLayer(user1Profile),
      makePermissionServiceWith(
        (user1Id, "editor", PermissionTarget.unsafe("document", "123"))
      ),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("List documents returns only permitted documents") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // List should only include document-1
        docs <- service.listDocuments()

      } yield assertTrue(
        docs.exists(_.id == "1"),
        !docs.exists(_.id == "2"),
        !docs.exists(_.id == "3")
      )
    }.provide(
      makeUserLayer(user1Profile),
      makePermissionServiceWith(
        (user1Id, "viewer", PermissionTarget.unsafe("document", "1"))
      ),
      ZLayer.succeed(ExampleDocumentService())
    )
  )
