// PURPOSE: Tapir endpoint definitions for ExampleDocumentService demonstrating authorization integration
// PURPOSE: Shows how to wire Tapir endpoints to service methods protected by Authorization.require guards

package works.iterative.server.http

import zio.*
import zio.json.*
import sttp.capabilities.zio.ZioStreams
import works.iterative.tapir.CustomTapir.{*, given}
import works.iterative.core.auth.*
import works.iterative.core.auth.service.AuthenticationService
import works.iterative.core.ExampleDocumentService
import works.iterative.core.Document

/** Tapir endpoints for ExampleDocumentService.
  *
  * This object demonstrates how to integrate Tapir endpoints with services
  * that use Authorization.require guards. Key patterns:
  *
  * 1. Use CustomTapir.toApi for bearer auth and error handling
  * 2. Use apiLogic extension to provide CurrentUser context
  * 3. Service methods handle authorization internally via Authorization.require
  * 4. AuthenticationError automatically mapped to appropriate HTTP status codes
  *
  * Endpoint definitions:
  * - POST /documents - Create new document (authenticated only)
  * - PUT /documents/:id - Update document (requires edit permission)
  * - DELETE /documents/:id - Delete document (requires delete permission)
  * - GET /documents - List viewable documents (filtered by permissions)
  *
  * Example usage in TapirEndpointModule:
  * {{{
  *   val endpoints = List(
  *     ExampleDocumentEndpoints.create,
  *     ExampleDocumentEndpoints.update,
  *     ExampleDocumentEndpoints.delete,
  *     ExampleDocumentEndpoints.list
  *   )
  * }}}
  *
  * Authorization pattern:
  * The endpoints delegate to ExampleDocumentService methods which use
  * Authorization.require internally. This means:
  * - 401 Unauthorized if user not authenticated
  * - 403 Forbidden if user lacks required permission
  * - 200 OK if permission granted and operation succeeds
  *
  * Error handling:
  * AuthenticationError variants are automatically mapped to HTTP responses:
  * - Unauthenticated → 401 with JSON error body
  * - Forbidden → 403 with resource and action details
  * - InvalidToken/TokenExpired → 401
  */
object ExampleDocumentEndpoints:

  /** Request body for creating a document. */
  case class CreateDocumentRequest(title: String)

  object CreateDocumentRequest:
    given JsonCodec[CreateDocumentRequest] = DeriveJsonCodec.gen
    given Schema[CreateDocumentRequest] = Schema.derived

  /** Request body for updating a document. */
  case class UpdateDocumentRequest(title: String)

  object UpdateDocumentRequest:
    given JsonCodec[UpdateDocumentRequest] = DeriveJsonCodec.gen
    given Schema[UpdateDocumentRequest] = Schema.derived

  // JSON codec and schema for Document response
  given JsonCodec[Document] = DeriveJsonCodec.gen
  given Schema[Document] = Schema.derived

  /** Base endpoint with common path prefix. */
  private val baseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
    endpoint.in("documents")

  /** Create document endpoint: POST /documents
    *
    * Creates a new document owned by the authenticated user.
    * Requires authentication but no specific permission.
    *
    * Request: { "title": "Document Title" }
    * Response: Document object with generated ID
    */
  val create: ZServerEndpoint[AuthenticationService & ExampleDocumentService & MutablePermissionService & EnumerablePermissionService, ZioStreams] =
    baseEndpoint
      .post
      .in(jsonBody[CreateDocumentRequest].description("Document to create"))
      .out(jsonBody[Document].description("Created document"))
      .toApi[Unit]
      .apiLogic { req =>
        for {
          service <- ZIO.service[ExampleDocumentService]
          doc <- service.createDocument(req.title)
        } yield doc
      }

  /** Update document endpoint: PUT /documents/:id
    *
    * Updates a document's title. Requires "edit" permission on the document.
    * Returns 403 Forbidden if user lacks permission.
    *
    * Path param: document ID
    * Request: { "title": "New Title" }
    * Response: Updated document
    */
  val update: ZServerEndpoint[AuthenticationService & ExampleDocumentService & MutablePermissionService & EnumerablePermissionService, ZioStreams] =
    baseEndpoint
      .put
      .in(path[String]("id").description("Document ID"))
      .in(jsonBody[UpdateDocumentRequest].description("Updated document data"))
      .out(jsonBody[Document].description("Updated document"))
      .toApi[Unit]
      .apiLogic { case (id, req) =>
        for {
          service <- ZIO.service[ExampleDocumentService]
          doc <- service.updateDocument(id, req.title)
        } yield doc
      }

  /** Delete document endpoint: DELETE /documents/:id
    *
    * Deletes a document. Requires "delete" permission on the document.
    * In the default config, only document owners have delete permission.
    *
    * Path param: document ID
    * Response: 204 No Content on success
    */
  val delete: ZServerEndpoint[AuthenticationService & ExampleDocumentService & MutablePermissionService & EnumerablePermissionService, ZioStreams] =
    baseEndpoint
      .delete
      .in(path[String]("id").description("Document ID"))
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .toApi[Unit]
      .apiLogic { id =>
        for {
          service <- ZIO.service[ExampleDocumentService]
          _ <- service.deleteDocument(id)
        } yield ()
      }

  /** List documents endpoint: GET /documents
    *
    * Returns all documents the authenticated user can view.
    * The list is filtered by the PermissionService to only include
    * documents where the user has "view" permission.
    *
    * Response: Array of Document objects
    */
  val list: ZServerEndpoint[AuthenticationService & ExampleDocumentService & MutablePermissionService & EnumerablePermissionService, ZioStreams] =
    baseEndpoint
      .get
      .out(jsonBody[Seq[Document]].description("List of viewable documents"))
      .toApi[Unit]
      .apiLogic { _ =>
        for {
          service <- ZIO.service[ExampleDocumentService]
          docs <- service.listDocuments()
        } yield docs
      }

  /** All endpoints as a list for easy registration. */
  val all: List[ZServerEndpoint[AuthenticationService & ExampleDocumentService & MutablePermissionService & EnumerablePermissionService, ZioStreams]] =
    List(create, update, delete, list)

end ExampleDocumentEndpoints
