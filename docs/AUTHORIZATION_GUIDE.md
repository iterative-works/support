# Authorization Guide

This guide explains how to use the authorization system in the IW Support library. The system provides declarative permission checking based on relationship-based access control (ReBAC), inspired by Google's Zanzibar.

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Permission Model](#permission-model)
3. [Authorization Patterns](#authorization-patterns)
4. [Tapir Endpoint Integration](#tapir-endpoint-integration)
5. [Testing Authorization](#testing-authorization)
6. [Configuration](#configuration)
7. [Common Scenarios](#common-scenarios)

## Core Concepts

### Permission Tuples

The authorization system is based on **relation tuples** of the form:

```
(user, relation, target)
```

Examples:
- `(alice, owner, document:123)` - Alice owns document 123
- `(bob, editor, document:456)` - Bob can edit document 456
- `(carol, viewer, folder:789)` - Carol can view folder 789

### Permission Hierarchy

Permissions can inherit from each other. For example, the default document configuration:

```scala
owner → edit → view
      ↘ delete
```

This means:
- An `owner` has `edit`, `view`, and `delete` permissions
- An `editor` has `view` permission
- A `viewer` has only `view` permission

### Permission Targets

Permission targets use the format: `namespace:objectId`

Examples:
- `document:123` - Document with ID "123"
- `folder:abc` - Folder with ID "abc"
- `task_list:xyz-456` - Task list with ID "xyz-456"

**Namespace rules:**
- Lowercase letters, numbers, and underscores only
- Must start with a letter
- Maximum 50 characters
- Cannot be empty

## Permission Model

### PermissionOp

Represents an operation/action to check:

```scala
val viewOp = PermissionOp.unsafe("view")
val editOp = PermissionOp.unsafe("edit")
val deleteOp = PermissionOp.unsafe("delete")
```

### PermissionTarget

Represents a resource to check permission on:

```scala
// Safe construction (validates format)
val target = PermissionTarget("document", "123") // Returns Validated[PermissionTarget]

// Unsafe construction (throws on invalid input)
val target = PermissionTarget.unsafe("document", "123")
```

### PermissionService

The core service interface for checking permissions:

```scala
trait PermissionService:
  def isAllowed(
    subj: Option[UserInfo],
    action: PermissionOp,
    obj: PermissionTarget
  ): UIO[Boolean]

  def listAllowed(
    subj: UserInfo,
    action: PermissionOp,
    namespace: String
  ): UIO[Set[String]]
```

## Authorization Patterns

### Pattern 1: Authorization.require

Use `Authorization.require` to protect individual operations:

```scala
import works.iterative.core.auth.Authorization

def updateDocument(id: String, title: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Document] =
  Authorization.require(
    PermissionOp.unsafe("edit"),
    PermissionTarget.unsafe("document", id)
  ) {
    // This block only executes if permission is granted
    for {
      doc <- repository.findById(id)
      updated = doc.copy(title = title)
      _ <- repository.save(updated)
    } yield updated
  }
```

**When to use:**
- Single resource operations (update, delete, view)
- Operations requiring specific permissions
- Clear permission failure handling (returns 403 Forbidden)

### Pattern 2: Authorization.filterAllowed

Use `Authorization.filterAllowed` for list queries:

```scala
def listDocuments(): ZIO[CurrentUser & PermissionService, Nothing, Seq[Document]] =
  for {
    allDocs <- repository.findAll()
    filtered <- Authorization.filterAllowed(
      PermissionOp.unsafe("view"),
      allDocs
    )(doc => PermissionTarget.unsafe("document", doc.id))
  } yield filtered
```

**When to use:**
- Listing resources
- Filtering collections by permission
- Authorization-aware queries

**Performance note:** Uses `PermissionService.listAllowed` internally for efficient batch checking.

### Pattern 3: Authorization.check

Use `Authorization.check` when you need the boolean result:

```scala
def canUserDelete(documentId: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Boolean] =
  Authorization.check(
    PermissionOp.unsafe("delete"),
    PermissionTarget.unsafe("document", documentId)
  )
```

**When to use:**
- UI rendering (show/hide buttons)
- Complex authorization logic
- Checking multiple permissions

## Tapir Endpoint Integration

The system integrates seamlessly with Tapir endpoints using the `.apiLogic` extension.

### Basic Endpoint Pattern

```scala
import works.iterative.tapir.CustomTapir.{*, given}
import works.iterative.core.auth.*

object DocumentEndpoints:
  // Update endpoint - requires edit permission
  val update: ZServerEndpoint[AuthenticationService & DocumentService & PermissionService, ZioStreams] =
    endpoint
      .put
      .in("documents" / path[String]("id"))
      .in(jsonBody[UpdateRequest])
      .out(jsonBody[Document])
      .toApi[Unit]  // Enables bearer auth and error handling
      .apiLogic { case (id, req) =>
        for {
          service <- ZIO.service[DocumentService]
          doc <- service.updateDocument(id, req.title)  // Service handles authorization
        } yield doc
      }
```

### How It Works

1. `.toApi[Unit]` adds:
   - Bearer token authentication
   - `CurrentUser` context provisioning
   - Automatic error mapping (AuthenticationError → HTTP status)

2. `.apiLogic` extension:
   - Provides `CurrentUser` to the service layer
   - Handles `AuthenticationError` automatically:
     - `Unauthenticated` → 401 Unauthorized
     - `Forbidden` → 403 Forbidden with resource details
     - `InvalidToken` → 401 Unauthorized

3. Service layer uses `Authorization.require`:
   - Checks permission using `CurrentUser` and `PermissionService`
   - Throws `AuthenticationError.Forbidden` if denied
   - Error automatically mapped to HTTP 403 by Tapir

### Complete Example

```scala
// Service with authorization
class DocumentService:
  def updateDocument(id: String, title: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Document] =
    Authorization.require(
      PermissionOp.unsafe("edit"),
      PermissionTarget.unsafe("document", id)
    ) {
      // Update logic here
      ZIO.succeed(Document(id, title, "owner-id"))
    }

// Tapir endpoint
object DocumentEndpoints:
  val update = endpoint
    .put
    .in("documents" / path[String]("id"))
    .in(jsonBody[UpdateRequest])
    .out(jsonBody[Document])
    .toApi[Unit]
    .apiLogic { case (id, req) =>
      for {
        service <- ZIO.service[DocumentService]
        doc <- service.updateDocument(id, req.title)
      } yield doc
    }
```

### Error Handling

Errors flow through the system automatically:

```
Service throws AuthenticationError.Forbidden("document:123", "edit")
         ↓
Tapir intercepts error (via .toApi)
         ↓
AuthErrorHandler maps to HTTP response
         ↓
Client receives 403 Forbidden with JSON:
{
  "error": "Forbidden",
  "resourceType": "document",
  "action": "edit"
}
```

## Testing Authorization

### Test Services

Use `TestAuthenticationService` and `InMemoryPermissionService`:

```scala
import works.iterative.core.auth.service.TestAuthenticationService
import works.iterative.core.auth.InMemoryPermissionService

// Create test user
val user = BasicProfile(
  subjectId = UserId.unsafe("test-user"),
  userName = Some(UserName.unsafe("Test User")),
  email = Some(Email.unsafe("test@example.com")),
  avatar = None,
  roles = Set.empty,
  claims = Set.empty
)

// Provide CurrentUser layer
def makeUserLayer(profile: BasicProfile): ULayer[CurrentUser] =
  ZLayer.succeed(CurrentUser(profile))

// Provide PermissionService with initial permissions
def makePermissionServiceWith(
  grants: (UserId, String, PermissionTarget)*
): ZLayer[Any, Nothing, PermissionService] =
  ZLayer.fromZIO {
    for {
      permService <- InMemoryPermissionService.make(PermissionConfig.default)
      _ <- ZIO.foreach(grants) { case (userId, relation, target) =>
        permService.addRelation(userId, relation, target)
      }
    } yield permService
  }
```

### Integration Test Example

```scala
test("User with editor permission can edit but not delete") {
  for {
    service <- ZIO.service[DocumentService]

    // Edit should succeed
    updated <- service.updateDocument("123", "New Title")

    // Delete should fail with Forbidden
    deleteResult <- service.deleteDocument("123").either

  } yield assertTrue(
    updated.title == "New Title",
    deleteResult.isLeft,
    deleteResult.left.exists {
      case AuthenticationError.Forbidden(_, _) => true
      case _ => false
    }
  )
}.provide(
  makeUserLayer(testUser),
  makePermissionServiceWith(
    (testUser.subjectId, "editor", PermissionTarget.unsafe("document", "123"))
  ),
  ZLayer.succeed(DocumentService())
)
```

### Testing Unauthenticated Access

```scala
test("Unauthenticated user cannot access protected resource") {
  for {
    service <- ZIO.service[DocumentService]
    result <- service.updateDocument("123", "Hacked").either
  } yield assertTrue(
    result.isLeft,
    result.left.exists {
      case AuthenticationError.Unauthenticated(_) => true
      case _ => false
    }
  )
}.provide(
  ZLayer.succeed(CurrentUser(BasicProfile.anonymous)),  // No authenticated user
  makePermissionServiceWith(),
  ZLayer.succeed(DocumentService())
)
```

## Configuration

### Permission Namespaces

Configure permission inheritance per namespace:

```scala
val config = PermissionConfig(
  namespaces = Map(
    "document" -> NamespaceConfig(
      implications = Map(
        "owner" -> Set("edit", "view", "delete"),
        "editor" -> Set("view"),
        "viewer" -> Set()
      )
    ),
    "folder" -> NamespaceConfig(
      implications = Map(
        "owner" -> Set("manage", "read"),
        "member" -> Set("read"),
        "viewer" -> Set()
      )
    )
  )
)
```

### Default Configuration

The system provides a default configuration:

```scala
PermissionConfig.default  // Includes standard document permissions
```

## Common Scenarios

### Document Ownership

When a user creates a document, grant them owner permission:

```scala
def createDocument(title: String): ZIO[CurrentUser & PermissionService, AuthenticationError, Document] =
  for {
    user <- CurrentUser.get
    doc = Document(UUID.randomUUID().toString, title, user.subjectId.value)
    permService <- ZIO.service[PermissionService]

    // Grant owner permission
    _ <- permService.grantPermission(
      user.subjectId,
      "owner",
      PermissionTarget.unsafe("document", doc.id)
    )
  } yield doc
```

### Folder Sharing

Grant multiple users different permissions on a folder:

```scala
def shareFolder(folderId: String, userId: UserId, role: String): ZIO[PermissionService, Nothing, Unit] =
  for {
    permService <- ZIO.service[PermissionService]
    _ <- permService.grantPermission(
      userId,
      role,  // "owner", "member", or "viewer"
      PermissionTarget.unsafe("folder", folderId)
    )
  } yield ()
```

### Revoking Access

Remove a user's permission:

```scala
def revokeAccess(userId: UserId, documentId: String): ZIO[PermissionService, Nothing, Unit] =
  for {
    permService <- ZIO.service[PermissionService]
    _ <- permService.revokePermission(
      userId,
      "editor",  // or whatever relation to remove
      PermissionTarget.unsafe("document", documentId)
    )
  } yield ()
```

## Architecture Notes

### Why Tapir (Not Middleware/Typed Routes)

This system uses **Tapir endpoints** for HTTP integration, which provides:

1. **Compile-time safety** - Type-safe endpoint definitions
2. **Automatic documentation** - OpenAPI/Swagger generation
3. **Composable security** - `.toApi` extension handles auth uniformly
4. **Error mapping** - AuthenticationError → HTTP status codes

**Middleware vs Typed Routes distinction doesn't apply** - Tapir provides both benefits:
- Compile-time endpoint validation (typed routes benefit)
- Runtime request interception (middleware benefit)
- Automatic error handling (both)

### Error Flow

```
1. Service throws AuthenticationError.Forbidden
2. Tapir endpoint catches via .toApi error handler
3. AuthErrorHandler.mapAuthError converts to HTTP response
4. Client receives 403 Forbidden with JSON body
```

### When to Use Each Pattern

| Pattern | Use Case | Error Handling |
|---------|----------|----------------|
| `Authorization.require` | Single resource operations | Throws Forbidden |
| `Authorization.filterAllowed` | List filtering | Never fails |
| `Authorization.check` | Boolean checks | Throws Forbidden |

## Best Practices

1. **Use unsafe constructors in application code** - Validation is for user input
   ```scala
   // Good
   PermissionTarget.unsafe("document", documentId)

   // Overkill (unless documentId from user input)
   PermissionTarget("document", documentId).fold(
     error => ...,
     target => ...
   )
   ```

2. **Check permissions in service layer, not routes** - Routes handle HTTP, services handle business logic

3. **Use descriptive permission operations** - `"edit"`, not `"write"` or `"modify"`

4. **Grant permissions when resources are created** - Don't forget to make creator the owner

5. **Test both granted and denied cases** - Ensure authorization actually works

6. **Use InMemoryPermissionService for tests** - Fast and simple

7. **Document your permission model** - What relations exist? What do they mean?

## Further Reading

- See `ExampleDocumentService` for a complete working example
- See `ExampleDocumentEndpoints` for Tapir integration patterns
- See `AuthorizationIntegrationSpec` for comprehensive test examples
- See Phase 1-3 implementation tasks for system architecture details
