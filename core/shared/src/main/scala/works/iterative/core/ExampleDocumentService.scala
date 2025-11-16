// PURPOSE: Example service demonstrating authorization patterns with Authorization.require and Authorization.filterAllowed
// PURPOSE: Shows how to protect service methods with declarative permission guards for create, update, delete, and list operations

package works.iterative.core

import zio.*
import works.iterative.core.auth.*

/** Example document data model for authorization demonstration.
  *
  * @param id Document unique identifier
  * @param title Document title
  * @param ownerId User ID of document owner
  */
case class Document(id: String, title: String, ownerId: String)

/** Example service demonstrating authorization guard patterns.
  *
  * This service shows how to use Authorization.require and Authorization.filterAllowed
  * to protect service methods with declarative permission checks. It serves as a
  * reference implementation for developers integrating authorization into their services.
  *
  * Key patterns demonstrated:
  * - createDocument: Authenticated-only access (no specific permission needed)
  * - updateDocument: Requires "edit" permission on specific document
  * - deleteDocument: Requires "delete" permission on specific document
  * - listDocuments: Filters results to show only documents user can view
  *
  * Authorization pattern:
  * {{{
  *   // 1. Define permission target for the resource
  *   val target = documentTarget(documentId)
  *
  *   // 2. Wrap protected operation with Authorization.require
  *   Authorization.require(PermissionOp.unsafe("edit"), target) {
  *     // Protected operation only executes if permission granted
  *     performUpdate(documentId, newData)
  *   }
  * }}}
  *
  * Error handling:
  * - Authorization.require returns ZIO[R, AuthenticationError, A]
  * - AuthenticationError.Forbidden thrown when permission denied
  * - Errors propagate to HTTP layer where they map to 403 Forbidden
  *
  * Usage example:
  * {{{
  *   val service = ExampleDocumentService()
  *
  *   // Create document (authenticated user becomes owner)
  *   for {
  *     doc <- service.createDocument("My Document")
  *
  *     // Update requires edit permission
  *     _ <- service.updateDocument(doc.id, "Updated Title")
  *
  *     // List shows only permitted documents
  *     docs <- service.listDocuments()
  *   } yield docs
  * }}}
  */
class ExampleDocumentService():

  /** Helper to create permission target for a document.
    *
    * This helper centralizes permission target construction, ensuring
    * consistent namespace usage and reducing duplication.
    *
    * @param id Document ID
    * @return PermissionTarget for the document
    */
  private def documentTarget(id: String): PermissionTarget =
    PermissionTarget.unsafe("document", id)

  /** Create a new document owned by the current user.
    *
    * This method demonstrates authenticated-only access - any authenticated user
    * can create a document. The current user automatically becomes the owner.
    *
    * @param title Document title
    * @return ZIO effect producing created Document
    */
  def createDocument(title: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Document] =
    for {
      currentUser <- ZIO.service[CurrentUser]

      // For creation, we just need an authenticated user
      // The user becomes the owner of the created document
      doc = Document(
        id = java.util.UUID.randomUUID().toString,
        title = title,
        ownerId = currentUser.subjectId.value
      )

      // Grant owner permission to the creator
      permService <- ZIO.service[PermissionService]
      impl = permService.asInstanceOf[InMemoryPermissionService]
      _ <- impl.addRelation(
        currentUser.subjectId,
        "owner",
        documentTarget(doc.id)
      )
    } yield doc

  /** Update a document's title.
    *
    * This method demonstrates resource-specific permission checking using
    * Authorization.require. The user must have "edit" permission on the
    * specific document to perform the update.
    *
    * @param id Document ID to update
    * @param newTitle New title for the document
    * @return ZIO effect producing updated Document or Forbidden error
    */
  def updateDocument(
      id: String,
      newTitle: String
  ): ZIO[CurrentUser & PermissionService, AuthenticationError, Document] =
    Authorization.require(PermissionOp.unsafe("edit"), documentTarget(id)) {
      ZIO.succeed(Document(id, newTitle, "owner-id"))
    }

  /** Delete a document.
    *
    * This method demonstrates delete permission checking. The user must have
    * "delete" permission on the specific document. In the default config,
    * only document owners have delete permission.
    *
    * @param id Document ID to delete
    * @return ZIO effect that succeeds if delete allowed, fails with Forbidden otherwise
    */
  def deleteDocument(id: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Unit] =
    Authorization.require(PermissionOp.unsafe("delete"), documentTarget(id)) {
      ZIO.unit // In real implementation, would delete from database
    }

  /** List all documents the current user can view.
    *
    * This method demonstrates Authorization.filterAllowed for efficient
    * permission-based filtering. Instead of checking permission on each
    * document individually, it uses a batch query to get all allowed
    * document IDs, then filters the list locally.
    *
    * In a real implementation, this would query the database for documents
    * and filter based on permissions. This example uses hard-coded test data.
    *
    * @return ZIO effect producing list of viewable documents
    */
  def listDocuments(): ZIO[CurrentUser & PermissionService, Nothing, Seq[Document]] =
    // Simulated document data (in real app, would come from database)
    val allDocuments = Seq(
      Document("1", "Document 1", "user-1"),
      Document("2", "Document 2", "user-2"),
      Document("3", "Document 3", "user-1")
    )

    // Filter to only documents user can view
    Authorization.filterAllowed(
      PermissionOp.unsafe("view"),
      allDocuments
    )(doc => documentTarget(doc.id))

end ExampleDocumentService

object ExampleDocumentService:
  /** Create an instance of ExampleDocumentService. */
  def apply(): ExampleDocumentService = new ExampleDocumentService()
end ExampleDocumentService
