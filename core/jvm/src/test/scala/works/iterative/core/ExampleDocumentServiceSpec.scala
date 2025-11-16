// PURPOSE: Test suite for ExampleDocumentService demonstrating authorization guard patterns
// PURPOSE: Validates Authorization.require and Authorization.filterAllowed usage with permission scenarios

package works.iterative.core

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.core.auth.*

object ExampleDocumentServiceSpec extends ZIOSpecDefault:

  // Test users
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

  def spec = suite("ExampleDocumentServiceSpec")(
    test("createDocument succeeds when user authenticated") {
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

    test("updateDocument succeeds when user has edit permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant edit permission to user1 for document-123
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

    test("updateDocument fails with Forbidden when user lacks permission") {
      for {
        service <- ZIO.service[ExampleDocumentService]

        // No permission granted - should fail
        exit <- service.updateDocument("456", "Unauthorized Update").exit
      } yield assertTrue(
        exit.isFailure,
        exit match {
          case Exit.Failure(cause) =>
            cause.failures.headOption match {
              case Some(AuthenticationError.Forbidden(resource, action)) =>
                resource == "456" && action == "edit"
              case _ => false
            }
          case _ => false
        }
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("deleteDocument requires delete permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant delete permission (owner relation gives delete permission)
        impl = permService.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(user1Id, "owner", PermissionTarget.unsafe("document", "789"))

        // Delete should succeed
        _ <- service.deleteDocument("789")
      } yield assertCompletes
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("deleteDocument fails when user lacks delete permission") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant only view permission (not delete)
        impl = permService.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(user1Id, "viewer", PermissionTarget.unsafe("document", "999"))

        // Delete should fail
        exit <- service.deleteDocument("999").exit
      } yield assertTrue(
        exit.isFailure,
        exit match {
          case Exit.Failure(cause) =>
            cause.failures.headOption match {
              case Some(AuthenticationError.Forbidden(resource, action)) =>
                resource == "999" && action == "delete"
              case _ => false
            }
          case _ => false
        }
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    ),

    test("listDocuments uses Authorization.filterAllowed to show only permitted documents") {
      for {
        permService <- ZIO.service[PermissionService]
        service <- ZIO.service[ExampleDocumentService]

        // Grant view permission to documents 1 and 3, but not 2
        impl = permService.asInstanceOf[InMemoryPermissionService]
        _ <- impl.addRelation(user1Id, "viewer", PermissionTarget.unsafe("document", "1"))
        _ <- impl.addRelation(user1Id, "viewer", PermissionTarget.unsafe("document", "3"))

        // List should return only documents 1 and 3
        result <- service.listDocuments()
      } yield assertTrue(
        result.size == 2,
        result.exists(_.id == "1"),
        result.exists(_.id == "3"),
        !result.exists(_.id == "2")
      )
    }.provide(
      makeUserLayer(user1Profile),
      ZLayer.fromZIO(InMemoryPermissionService.make(PermissionConfig.default)),
      ZLayer.succeed(ExampleDocumentService())
    )
  )
