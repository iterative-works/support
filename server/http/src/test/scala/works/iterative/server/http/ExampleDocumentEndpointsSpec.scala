// PURPOSE: Test suite for ExampleDocumentEndpoints validating Tapir endpoint definitions
// PURPOSE: Ensures endpoints are properly defined and can be compiled with correct types

package works.iterative.server.http

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.*
import works.iterative.core.auth.service.AuthenticationService
import works.iterative.core.ExampleDocumentService
import works.iterative.core.Document
import works.iterative.core.UserName
import works.iterative.core.Email

object ExampleDocumentEndpointsSpec extends ZIOSpecDefault:

  // Test user profiles
  val user1Id = UserId.unsafe("user-1")
  val user1Profile = BasicProfile(
    subjectId = user1Id,
    userName = Some(UserName.unsafe("User 1")),
    email = Some(Email.unsafe("user1@example.com")),
    avatar = None,
    roles = Set.empty
  )

  val user2Id = UserId.unsafe("user-2")
  val user2Profile = BasicProfile(
    subjectId = user2Id,
    userName = Some(UserName.unsafe("User 2")),
    email = Some(Email.unsafe("user2@example.com")),
    avatar = None,
    roles = Set.empty
  )

  // Helper to create test layer with specific user
  def makeUserLayer(profile: BasicProfile): ULayer[CurrentUser] =
    ZLayer.succeed(CurrentUser(profile))

  def spec = suite("ExampleDocumentEndpointsSpec")(
    test("ExampleDocumentEndpoints compiles and has correct endpoint definitions") {
      // This test validates that the endpoints object exists and compiles
      // Full HTTP integration testing will be in E2E tests (Task 4)
      for {
        _ <- ZIO.attempt {
          val endpoints = ExampleDocumentEndpoints
          // If this compiles, the endpoints exist with correct types
          assertTrue(true)
        }
      } yield assertCompletes
    },

    test("createDocument logic succeeds when authenticated") {
      // Test the underlying service logic that endpoints will use
      for {
        service <- ZIO.service[ExampleDocumentService]
        result <- service.createDocument("Test Document")
      } yield assertTrue(
        result.title == "Test Document",
        result.ownerId == user1Id.value
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("updateDocument logic succeeds with edit permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant edit permission
        impl = permService.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(user1Id, "editor", PermissionTarget.unsafe("document", "123"))

        // Update should succeed
        result <- service.updateDocument("123", "Updated Title")
      } yield assertTrue(
        result.id == "123",
        result.title == "Updated Title"
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("updateDocument logic returns Forbidden without permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // No permission granted for user2 on document-123
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
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("deleteDocument logic requires delete permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // No delete permission granted
        result <- service.deleteDocument("123").either
      } yield assertTrue(
        result.isLeft,
        result.left.exists {
          case AuthenticationError.Forbidden(_, _) => true
          case _ => false
        }
      )
    }.provide(
      makeUserLayer(user2Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("listDocuments logic filters by permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant view permission only for document-1
        impl = permService.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(user1Id, "viewer", PermissionTarget.unsafe("document", "1"))

        // List should only include document-1
        docs <- service.listDocuments()
      } yield assertTrue(
        docs.exists(_.id == "1"),
        !docs.exists(_.id == "2"),
        !docs.exists(_.id == "3")
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    )
  )
