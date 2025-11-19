# Authentication and Authorization Research for HTTP4S Server with ZIO

## Executive Summary

This document presents research and recommendations for improving our authentication and authorization system for HTTP4S applications with ZIO. The proposed architecture addresses four key requirements:

1. **Pluggable authentication** supporting both OIDC (production) and simple account picker (testing)
2. **Universal user information model** with extensibility for project-specific data
3. **ZIO-native user context access** throughout the application
4. **Zanzibar-inspired authorization** using relationship-based access control (ReBAC) instead of hardcoded roles

**Key Discovery:** Our codebase already contains a Zanzibar-like authorization framework (`PermissionService`, `PermissionTarget`) that provides fine-grained, relationship-based access control. The recommendations focus on integrating this system with authentication and providing implementations.

## Current State Analysis

### Existing Pac4J Integration

Our current implementation uses Pac4J for authentication with the following components:

**Key Files:**
- `Pac4jHttpSecurity.scala` - HTTP4S middleware integration with session management
- `Pac4jConfigFactory.scala` - OIDC client configuration
- `Pac4jModuleRegistry.scala` - Module-level authentication wrapper
- `Pac4jSecurityConfig.scala` - Configuration model

**Current Architecture:**
```scala
trait Pac4jModuleRegistry[R, U] extends ModuleRegistry[R]:
    def pac4jSecurity: Pac4jHttpSecurity[RIO[R, *]]
    def profileToUser(profile: List[CommonProfile]): Option[U]

    protected def wrapModule(
        protectedPath: String,
        module: AuthedZIOWebModule[R, U]
    ): ZIOWebModule[R]
```

**Strengths:**
- Already supports OIDC via Pac4J
- Has session management infrastructure
- Provides type-safe user context via `AuthedZIOWebModule[R, C]`
- Separation between authenticated and public routes

**Limitations:**
1. **Tightly coupled to Pac4J** - Not easy to swap authentication providers
2. **Java-centric API** - Pac4J is Java-based, requiring adapters for functional Scala/ZIO
3. **Limited ZIO integration** - User context not available via ZIO environment
4. **No test authentication mode** - Only supports real OIDC, making testing difficult
5. **Inflexible user model** - Relies on Pac4J's `CommonProfile`, limiting extensibility

### Existing User Model and Authentication Service

**Key Files:**
- `core/shared/src/main/scala/works/iterative/core/auth/UserInfo.scala` - Minimal user trait
- `core/shared/src/main/scala/works/iterative/core/auth/UserProfile.scala` - Extended user interface
- `core/shared/src/main/scala/works/iterative/core/auth/BasicProfile.scala` - Concrete user implementation
- `core/shared/src/main/scala/works/iterative/core/auth/CurrentUser.scala` - ZIO service wrapper
- `core/shared/src/main/scala/works/iterative/core/auth/service/AuthenticationService.scala` - Authentication service

**Current User Hierarchy:**
```scala
trait UserInfo:
    def subjectId: UserId

trait UserRoles extends UserInfo:
    def roles: Set[UserRole]

trait UserProfile extends UserRoles:
    def userName: Option[UserName]
    def email: Option[Email]
    def avatar: Option[Avatar]
    def claims: Set[Claim]

case class BasicProfile(...) extends UserProfile

case class CurrentUser(userProfile: BasicProfile) extends UserProfile
```

**Authentication Service (Already Exists!):**
```scala
trait AuthenticationService:
    def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit]
    def currentUserInfo: UIO[Option[AuthedUserInfo]]
    def provideCurrentUser[R, E, A](
        effect: ZIO[R & CurrentUser, E, A]
    ): ZIO[R, E | AuthenticationError, A]

object FiberRefAuthentication extends AuthenticationService  // FiberRef-based
object GlobalRefAuthentication extends AuthenticationService  // Global Ref-based
```

**Strengths:**
- ZIO-native user context via `CurrentUser` service
- FiberRef-based implementation for fiber-local context
- Type-safe user information model
- Extensible via `Claim` system
- Already provides `provideCurrentUser` for ZIO environment

**Gaps:**
- Not integrated with Pac4J authentication
- No test implementation for easy testing
- Roles are present but not well-utilized

### Existing Zanzibar-like Authorization System

**Key Discovery:** The codebase contains a sophisticated Relationship-Based Access Control (ReBAC) system inspired by Google's Zanzibar, but it lacks implementation.

**Key Files:**
- `core/shared/src/main/scala/works/iterative/core/auth/PermissionService.scala` - Authorization service interface
- `core/shared/src/main/scala/works/iterative/core/Action.scala` - Action combining operation and target

**Permission Model:**
```scala
// Permission operation (e.g., "read", "write", "delete")
opaque type PermissionOp = String

// Permission target in format: namespace:id#rel
// Examples: "document:123", "document:123#owner", "folder:456#member"
opaque type PermissionTarget = String

object PermissionTarget:
    def apply(namespace: String, id: String, rel: Option[String]): Validated[PermissionTarget]
    def unsafe(namespace: String, id: String, rel: Option[String] = None): PermissionTarget

    extension (target: PermissionTarget)
        def namespace: String      // e.g., "document"
        def value: String          // e.g., "123"
        def rel: Option[String]    // e.g., Some("owner")

// Trait for domain objects that can be permission targets
trait Targetable:
    def permissionTarget: PermissionTarget

// Authorization service
trait PermissionService:
    def isAllowed(
        subj: Option[UserInfo],
        action: PermissionOp,
        obj: PermissionTarget
    ): UIO[Boolean]

// Combined action
case class Action(op: PermissionOp, target: PermissionTarget)
```

**Zanzibar Concepts in Our System:**

| Zanzibar Concept | Our Implementation | Example |
|------------------|-------------------|---------|
| Relation Tuples (object#relation@user) | `PermissionTarget` (namespace:id#rel) | `document:123#owner` |
| Check API | `PermissionService.isAllowed` | Check if user can "read" "document:123" |
| Namespace Configuration | Not yet implemented | Define rules for permission inheritance |
| Usersets | Not yet implemented | Groups and role memberships |
| Relationship Graph | Not yet implemented | Traverse relationships for authorization |

**Strengths:**
- Clean, type-safe API design
- Zanzibar-inspired ReBAC model
- Supports relationship-based permissions (via `#rel` syntax)
- Generic enough for any authorization pattern
- `Targetable` trait allows domain objects to define their permission target

**Gaps:**
1. **No implementation** - `PermissionService` is just a trait
2. **No relationship storage** - Need to store and query permission tuples
3. **No permission rules** - Need namespace configuration for inheritance rules
4. **Not integrated with authentication** - No connection to `CurrentUser`
5. **No test implementation** - Makes testing authorization difficult

## Google Zanzibar Overview

Google Zanzibar is a global authorization system managing permissions across all Google services. Understanding its concepts helps us implement our own system correctly.

### Core Concepts

**Relation Tuples:**
- Format: `object#relation@user` or `object#relation@group#member`
- Examples:
  - `document:readme#owner@alice` - Alice owns the readme document
  - `document:readme#viewer@group:engineers#member` - Engineers group members can view
  - `folder:docs#parent@document:readme` - Readme is in docs folder

**Check Operation:**
```
Check(object#relation@user) → Boolean
```
Traverses the relationship graph to determine if the user has the specified relation to the object.

**Permission Inheritance:**
- Direct relationships: `document:readme#owner@alice`
- Computed relationships: `document:readme#viewer@alice` (if alice is owner and owners can view)
- Transitive relationships: Through parent/child relationships

### Key Features

1. **Consistency (Zookies):** Ensures authorization checks see up-to-date permission data
2. **Performance:** Sub-10ms response for 95% of checks at Google scale
3. **Availability:** 99.999% uptime
4. **Global Scale:** Handles trillions of ACLs across all Google services

### APIs

1. **Write:** Add/remove relationship tuples
2. **Read:** Query existing relationships
3. **Check:** Verify user has permission
4. **Expand:** Get all users with a permission
5. **Watch:** Monitor permission changes

### Open Source Alternatives

- **SpiceDB** - Full Zanzibar implementation in Go
- **OpenFGA** - Auth0's Zanzibar implementation
- **Oso** - Policy-based authorization with ReBAC support
- **Keto** - Ory's Zanzibar implementation

## Authentication Best Practices Research

### HTTP4S Authentication Patterns

**Middleware-Based Authentication:**
- Use `AuthMiddleware` to intercept requests before they reach business logic
- Extract tokens from headers/cookies and validate them
- Transform `Request[F]` to `AuthedRequest[F, User]` for authenticated routes
- Apply middleware selectively to protected endpoints

**Integration with Standard Protocols:**
- Support Bearer tokens for OAuth2/OIDC
- Validate JWTs by verifying signature against provider's JWKS endpoint
- Query authorization server for token validation
- Use ZIO effects for async validation

**Pluggable Design:**
```scala
type AuthService = Request[Task] => IO[AuthError, User]
```
- Abstract authentication behind interfaces
- Inject different implementations via configuration
- Use ZIO layers for dependency injection

### ZIO Authentication Patterns

**User Context Management:**
- Use `AuthedRequest[F, User]` to carry user identity through endpoints
- Store context in **ZIO FiberRef** or **ZIO environment layer**
- Make user information available via `ZIO.service[UserContext]`
- Avoid global state; extract context per-request

**Handler Aspects (ZIO HTTP pattern):**
```scala
HandlerAspect.customAuthProviding[UserContext] { req =>
    // Validate, parse claims, etc.
    Some(UserContext(...))
}
```

**Role-Based Access Control:**
```scala
for {
    ctx <- ZIO.service[UserContext]
    _   <- ZIO.fail(AuthError.Forbidden).unless(ctx.roles.contains("admin"))
    // proceed
} yield ...
```

### Pac4J Analysis

**Pros:**
- Broad protocol support (OAuth, SAML, OIDC, CAS, etc.)
- Framework-agnostic, works with many JVM frameworks
- Highly customizable with extension points
- Caching support for performance optimization

**Cons:**
- Java-centric API, not idiomatic for Scala/ZIO
- Requires wrapper code for ZIO integration
- Complex configuration can be verbose
- Performance overhead for direct clients without caching
- No native support for ZIO's effect system

**Alternatives:**
- **Native ZIO solution** - Build our own abstraction
- **zio-http built-in auth** - Limited but ZIO-native
- **play-pac4j** - Play-specific (not applicable)
- **http4s-auth** - Basic, requires extension

**Recommendation:** Continue using Pac4J for OIDC but abstract it behind our own interfaces to enable swappable implementations.

## Security Best Practices

### Token Validation

**Critical checks for JWT tokens:**
1. **Signature verification** - Validate cryptographic signature using provider's JWKS
2. **Algorithm validation** - Ensure approved signing algorithm, reject "none"
3. **Claims validation:**
   - `iss` (issuer) - Must match trusted issuer URL (HTTPS only)
   - `aud` (audience) - Must match client identifier
   - `exp` (expiration) - Reject expired tokens with clock skew tolerance
   - `nbf` (not before) - Validate token is active
   - `nonce` - Verify nonce to prevent replay attacks
   - `iat` (issued at), `jti` (token ID) - Optional but recommended

4. **Key rotation** - Update JWKS cache regularly, don't cache indefinitely

### Session Management

- **Secure cookies:** HTTP-only, Secure flag, SameSite=Strict
- **Short session lifespans** - Require periodic re-authentication
- **Session binding** - Tie to client IP/user-agent where appropriate
- **Proper logout** - Implement RP-initiated logout and token revocation

### CSRF Protection

- **State parameter** - Generate cryptographically random state per request
- **SameSite cookies** - Prevent cross-site request forgery
- **Anti-CSRF tokens** - For forms and sensitive endpoints

### Common Vulnerabilities

| Vulnerability | Prevention |
|---------------|------------|
| Token replay/session fixation | Use nonce, validate state and jti, revoke on logout |
| Code injection | Validate state and redirect_uri, HTTPS only |
| Token substitution | Strong nonce, bind tokens to session |
| Leaked tokens in URLs | Use Authorization Code Flow with PKCE |
| XSS/CSRF token disclosure | HTTP-only cookies, never expose to JavaScript |
| Algorithm downgrade | Whitelist approved algorithms only |
| Open redirects | Strict redirect_uri validation with whitelist |

### Client Secret Management

- **Never expose in client-side code** - Server-side only
- **Use secure storage** - Environment variables, vaults, encrypted secrets
- **Minimal access** - Restrict to services that need them
- **Regular rotation** - Rotate periodically and on suspected compromise
- **PKCE for public clients** - Replace secrets for SPAs/mobile apps

## Testing Best Practices

### Test Authentication Implementation

**Account Picker Pattern:**
- Simple UI or API allowing selection of predefined test users
- Each user has known roles, permissions, and attributes
- Only enabled in non-production environments
- Controlled via environment variables

**Environment-Based Configuration:**
```scala
sealed trait AuthProvider
case class OidcAuth(config: OidcConfig) extends AuthProvider
case class TestAuth(users: List[TestUser]) extends AuthProvider

def loadAuthProvider(env: Environment): AuthProvider =
    if (env.isProd) loadOidcAuth() else loadTestAuth()
```

**Abstraction Layer:**
```scala
trait AuthenticationService:
    def authenticate(request: Request): IO[AuthError, UserContext]

class OidcAuthService extends AuthenticationService
class TestAuthService extends AuthenticationService
```

### Security Testing

**Active Testing:**
- Attempt authentication bypass
- Inject malformed credentials
- Replay tokens
- Manipulate sessions
- Test MFA flows
- Brute-force attempts

**Passive Testing:**
- Monitor HTTP exchanges
- Check session token attributes
- Analyze error responses for information leaks

**OWASP Guidelines:**
- Follow Web Security Testing Guide
- Regular penetration testing
- Code reviews of authentication components

### Test Context Management

- **Inject user context** for different roles and permissions
- **Use account picker** with well-known test credentials
- **Pre-populate sessions** for browser tests
- **Isolate contexts** between tests to prevent leakage

## Proposed Architecture

**Note:** The following architecture builds on existing systems rather than replacing them:
- User model (`UserInfo`, `UserProfile`, `BasicProfile`) already exists - we'll use it
- `AuthenticationService` already exists with FiberRef implementation - we'll extend it
- `PermissionService` interface already exists - we need to implement it
- `CurrentUser` ZIO service already exists - we'll integrate with it

The focus is on:
1. **Implementing `PermissionService`** with both production and test backends
2. **Integrating Pac4J with existing `AuthenticationService`**
3. **Adding test authentication provider** for easy testing
4. **Connecting authorization checks** to business logic via ZIO environment

### 1. Implementing PermissionService (Zanzibar-like Authorization)

The existing `PermissionService` trait provides the interface; we need implementations.

**In-Memory Implementation (for testing and simple cases):**

```scala
// PURPOSE: In-memory permission storage for testing and development
// PURPOSE: Implements Zanzibar-style relationship-based access control

case class RelationTuple(
    user: UserId,
    relation: String,
    target: PermissionTarget
)

class InMemoryPermissionService(
    tuples: Ref[Set[RelationTuple]],
    config: PermissionConfig
) extends PermissionService:

    override def isAllowed(
        subj: Option[UserInfo],
        action: PermissionOp,
        obj: PermissionTarget
    ): UIO[Boolean] =
        subj match
            case None => ZIO.succeed(false)  // Unauthenticated users have no permissions
            case Some(user) =>
                checkPermission(user.subjectId, action, obj)

    private def checkPermission(
        userId: UserId,
        action: PermissionOp,
        target: PermissionTarget
    ): UIO[Boolean] =
        for {
            allTuples <- tuples.get
            // Check direct permission
            hasDirect = allTuples.exists(t =>
                t.user == userId &&
                t.relation == action.value &&
                t.target == target
            )
            // Check via computed relations (e.g., owner implies viewer)
            hasComputed <- if (!hasDirect) checkComputedRelation(userId, action, target, allTuples)
                          else ZIO.succeed(false)
        } yield hasDirect || hasComputed

    private def checkComputedRelation(
        userId: UserId,
        action: PermissionOp,
        target: PermissionTarget,
        allTuples: Set[RelationTuple]
    ): UIO[Boolean] =
        // Example: if user is "owner" and action is "view", check if owner implies view
        config.getImpliedRelations(target.namespace, action.value).foldLeft(ZIO.succeed(false)) {
            (acc, impliedBy) =>
                acc.flatMap { result =>
                    if (result) ZIO.succeed(true)
                    else ZIO.succeed(allTuples.exists(t =>
                        t.user == userId &&
                        t.relation == impliedBy &&
                        t.target == target
                    ))
                }
        }

    // Management API
    def addRelation(tuple: RelationTuple): UIO[Unit] =
        tuples.update(_ + tuple)

    def removeRelation(tuple: RelationTuple): UIO[Unit] =
        tuples.update(_ - tuple)

    def getRelations(userId: UserId): UIO[Set[RelationTuple]] =
        tuples.get.map(_.filter(_.user == userId))
end InMemoryPermissionService

object InMemoryPermissionService:
    def layer: ZLayer[PermissionConfig, Nothing, PermissionService] =
        ZLayer.fromZIO {
            for {
                config <- ZIO.service[PermissionConfig]
                ref <- Ref.make(Set.empty[RelationTuple])
            } yield InMemoryPermissionService(ref, config)
        }
```

**Permission Configuration:**

```scala
// PURPOSE: Configure permission rules for each namespace
// PURPOSE: Define which relations imply other relations (e.g., owner implies viewer)

case class PermissionConfig(
    namespaces: Map[String, NamespaceConfig]
):
    def getImpliedRelations(namespace: String, relation: String): Set[String] =
        namespaces.get(namespace)
            .flatMap(_.implications.get(relation))
            .getOrElse(Set.empty)

case class NamespaceConfig(
    name: String,
    // Map of relation -> set of relations it implies
    // Example: "owner" -> Set("editor", "viewer")
    implications: Map[String, Set[String]]
)

object PermissionConfig:
    // Example configuration for a document management system
    val documentSystem = PermissionConfig(
        Map(
            "document" -> NamespaceConfig(
                "document",
                Map(
                    "owner" -> Set("edit", "view", "delete"),
                    "editor" -> Set("view", "edit"),
                    "viewer" -> Set("view")
                )
            ),
            "folder" -> NamespaceConfig(
                "folder",
                Map(
                    "owner" -> Set("edit", "view", "delete", "manage"),
                    "editor" -> Set("view", "edit"),
                    "viewer" -> Set("view")
                )
            )
        )
    )
```

**Database-Backed Implementation (for production):**

```scala
// PURPOSE: Persistent permission storage using database
// PURPOSE: Production-ready implementation with proper indexing

class DatabasePermissionService(
    repository: PermissionRepository,
    config: PermissionConfig
) extends PermissionService:

    override def isAllowed(
        subj: Option[UserInfo],
        action: PermissionOp,
        obj: PermissionTarget
    ): UIO[Boolean] =
        subj match
            case None => ZIO.succeed(false)
            case Some(user) =>
                repository.hasPermission(user.subjectId, action, obj, config)
                    .catchAll(_ => ZIO.succeed(false))  // Fail closed on errors

trait PermissionRepository:
    def hasPermission(
        userId: UserId,
        action: PermissionOp,
        target: PermissionTarget,
        config: PermissionConfig
    ): IO[RepositoryError, Boolean]

    def addRelation(userId: UserId, relation: String, target: PermissionTarget): IO[RepositoryError, Unit]
    def removeRelation(userId: UserId, relation: String, target: PermissionTarget): IO[RepositoryError, Unit]
    def getUserRelations(userId: UserId): IO[RepositoryError, Set[(String, PermissionTarget)]]
    def getObjectRelations(target: PermissionTarget): IO[RepositoryError, Set[(UserId, String)]]
```

**Test Permission Service:**

```scala
// PURPOSE: Simple test implementation that always allows for testing
// PURPOSE: Or allows configuration of specific test permissions

class AlwaysAllowPermissionService extends PermissionService:
    override def isAllowed(
        subj: Option[UserInfo],
        action: PermissionOp,
        obj: PermissionTarget
    ): UIO[Boolean] = ZIO.succeed(true)

class ConfigurableTestPermissionService(
    allowedPermissions: Set[(UserId, PermissionOp, PermissionTarget)]
) extends PermissionService:
    override def isAllowed(
        subj: Option[UserInfo],
        action: PermissionOp,
        obj: PermissionTarget
    ): UIO[Boolean] =
        subj match
            case None => ZIO.succeed(false)
            case Some(user) =>
                ZIO.succeed(allowedPermissions.contains((user.subjectId, action, obj)))
```

### 2. Integrating Authentication with Authorization

**Declarative Authorization Guards:**

```scala
// PURPOSE: Provide declarative authorization checks using PermissionService
// PURPOSE: Type-safe, composable authorization for business logic

object Authorization:
    /** Require permission to perform action on target */
    def require[R, E](
        action: PermissionOp,
        target: PermissionTarget
    )(
        effect: ZIO[R & CurrentUser & PermissionService, E, *]
    ): ZIO[R & CurrentUser & PermissionService, E | AuthenticationError, Unit] =
        for {
            user <- CurrentUser.use(identity)
            allowed <- PermissionService.isAllowed(Some(user), action, target)
            _ <- if (allowed) effect
                else ZIO.fail(AuthenticationError(UserMessage("error.forbidden")))
        } yield ()

    /** Require permission, provide subject to effect */
    def withPermission[R, E, A](
        action: PermissionOp,
        target: PermissionTarget
    )(
        f: UserInfo => ZIO[R & PermissionService, E, A]
    ): ZIO[R & CurrentUser & PermissionService, E | AuthenticationError, A] =
        for {
            user <- CurrentUser.use(identity)
            allowed <- PermissionService.isAllowed(Some(user), action, target)
            result <- if (allowed) f(user)
                     else ZIO.fail(AuthenticationError(UserMessage("error.forbidden")))
        } yield result

    /** Check permission without failing */
    def check(
        action: PermissionOp,
        target: PermissionTarget
    ): URIO[CurrentUser & PermissionService, Boolean] =
        for {
            user <- CurrentUser.use(identity)
            allowed <- PermissionService.isAllowed(Some(user), action, target)
        } yield allowed

    /** Filter objects based on permissions */
    def filterAllowed[A <: Targetable](
        action: PermissionOp,
        objects: List[A]
    ): URIO[CurrentUser & PermissionService, List[A]] =
        for {
            user <- CurrentUser.use(identity)
            results <- ZIO.foreach(objects) { obj =>
                PermissionService.isAllowed(Some(user), action, obj.permissionTarget)
                    .map(allowed => if (allowed) Some(obj) else None)
            }
        } yield results.flatten
end Authorization
```

**Usage in Service Layer:**

```scala
// PURPOSE: Example service using permission-based authorization
// PURPOSE: Shows integration of PermissionService with business logic

class DocumentService(repository: DocumentRepository):

    /** Get document - requires view permission */
    def getDocument(id: DocId): ZIO[CurrentUser & PermissionService, AppError | AuthenticationError, Document] =
        val target = PermissionTarget.unsafe("document", id.value)
        Authorization.withPermission(PermissionOp("view"), target) { user =>
            repository.findById(id).someOrFail(AppError.NotFound(s"Document $id"))
        }

    /** Update document - requires edit permission */
    def updateDocument(id: DocId, content: String): ZIO[CurrentUser & PermissionService, AppError | AuthenticationError, Unit] =
        val target = PermissionTarget.unsafe("document", id.value)
        Authorization.require(PermissionOp("edit"), target) {
            repository.update(id, content)
        }

    /** Delete document - requires delete permission (typically only owner) */
    def deleteDocument(id: DocId): ZIO[CurrentUser & PermissionService, AppError | AuthenticationError, Unit] =
        val target = PermissionTarget.unsafe("document", id.value)
        Authorization.require(PermissionOp("delete"), target) {
            repository.delete(id)
        }

    /** List all documents user can view */
    def listDocuments: URIO[CurrentUser & PermissionService & Repository, List[Document]] =
        for {
            allDocs <- repository.findAll
            viewable <- Authorization.filterAllowed(PermissionOp("view"), allDocs)
        } yield viewable

    /** Create document - automatically grants owner permission */
    def createDocument(content: String): ZIO[CurrentUser & PermissionService & Repository, AppError, Document] =
        for {
            user <- CurrentUser.use(identity)
            doc <- repository.create(content, user.subjectId)
            // Grant owner permission
            _ <- addOwnerPermission(user.subjectId, doc.id)
        } yield doc

    private def addOwnerPermission(userId: UserId, docId: DocId): URIO[PermissionService, Unit] =
        // This requires extending PermissionService with write operations
        // Or using a separate PermissionManagementService
        ???
```

### 3. Pluggable Authentication Providers

**Integrating Pac4J with AuthenticationService:**

```scala
// PURPOSE: Adapt Pac4J authentication to our AuthenticationService
// PURPOSE: Bridge between HTTP4S/Pac4J and our domain model

class Pac4jAuthenticationAdapter(
    pac4jSecurity: Pac4jHttpSecurity[Task]
) extends AuthenticationService:

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        // Store in session or FiberRef
        FiberRef.currentUser.set(Some(AuthedUserInfo(token, profile)))

    override def currentUserInfo: UIO[Option[AuthedUserInfo]] =
        // Retrieve from session or FiberRef
        FiberRef.currentUser.get

    // Additional methods to integrate with Pac4J middleware
    def extractProfileFromPac4j(pac4jProfile: List[CommonProfile]): Option[BasicProfile] =
        pac4jProfile.headOption.map { profile =>
            BasicProfile(
                subjectId = UserId.unsafe(profile.getId),
                userName = Option(profile.getDisplayName).map(UserName.unsafe),
                email = Option(profile.getEmail).map(Email.unsafe),
                avatar = None,
                roles = profile.getRoles.asScala.map(UserRole.unsafe).toSet,
                claims = extractClaims(profile)
            )
        }

    private def extractClaims(profile: CommonProfile): Set[Claim] =
        profile.getAttributes.asScala.map {
            case (name, value: String) => Claim.StringClaim(name, value)
            case _ => ??? // Handle other types
        }.toSet
```

**Test Authentication Implementation:**

```scala
// PURPOSE: Simple test authentication with user picker
// PURPOSE: Enable easy testing without real OIDC

class TestAuthenticationService(
    testUsers: Map[UserId, BasicProfile],
    defaultUserId: Option[UserId]
) extends AuthenticationService:

    private val currentUserRef: FiberRef[Option[AuthedUserInfo]] =
        Unsafe.unsafely(FiberRef.unsafe.make(
            defaultUserId.flatMap(id => testUsers.get(id).map(p =>
                AuthedUserInfo(AccessToken("test-token"), p)
            ))
        ))

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        currentUserRef.set(Some(AuthedUserInfo(token, profile)))

    override def currentUserInfo: UIO[Option[AuthedUserInfo]] =
        currentUserRef.get

    // Test-specific method to switch users
    def loginAs(userId: UserId): IO[AuthenticationError, Unit] =
        testUsers.get(userId) match
            case Some(profile) => loggedIn(AccessToken(s"test-$userId"), profile)
            case None => ZIO.fail(AuthenticationError(UserMessage("error.unknown.test.user")))

object TestAuthenticationService:
    def withUsers(users: BasicProfile*): TestAuthenticationService =
        new TestAuthenticationService(
            users.map(u => u.subjectId -> u).toMap,
            users.headOption.map(_.subjectId)
        )

    val layer: ZLayer[Any, Nothing, AuthenticationService] =
        ZLayer.succeed(withUsers(
            BasicProfile(
                UserId.unsafe("test-admin"),
                Some(UserName.unsafe("Test Admin")),
                Some(Email.unsafe("admin@test.com")),
                None,
                Set(UserRole.unsafe("admin")),
                Set.empty
            ),
            BasicProfile(
                UserId.unsafe("test-user"),
                Some(UserName.unsafe("Test User")),
                Some(Email.unsafe("user@test.com")),
                None,
                Set(UserRole.unsafe("user")),
                Set.empty
            )
        ))
```

### 4. Complete Integration Example

```scala
// PURPOSE: Complete application setup showing integration of all components
// PURPOSE: Demonstrates authentication, authorization, and business logic working together

object MyApplication extends ZIOAppDefault:

    //Configuration layers
    val permissionConfig = ZLayer.succeed(PermissionConfig.documentSystem)

    // Service layers
    val permissionServiceLayer = permissionConfig >>> InMemoryPermissionService.layer
    val authServiceLayer = TestAuthenticationService.layer  // Or Pac4jAuthenticationAdapter.layer for production

    // Repository layer
    val documentRepositoryLayer = ZLayer.succeed(new InMemoryDocumentRepository())

    // Business logic layer
    val documentServiceLayer = ZLayer.fromFunction(DocumentService.apply _)

    // Application layer combining all dependencies
    val appLayer =
        permissionServiceLayer ++
        authServiceLayer ++
        documentRepositoryLayer ++
        documentServiceLayer

    // Example endpoint using the document service
    val documentRoutes: HttpRoutes[RIO[CurrentUser & PermissionService & DocumentService, *]] =
        HttpRoutes.of {
            case GET -> Root / "documents" / DocIdVar(id) =>
                for {
                    service <- ZIO.service[DocumentService]
                    doc <- service.getDocument(id)
                    response <- Ok(doc.toJson)
                } yield response

            case req @ PUT -> Root / "documents" / DocIdVar(id) =>
                for {
                    content <- req.as[String]
                    service <- ZIO.service[DocumentService]
                    _ <- service.updateDocument(id, content)
                    response <- Ok()
                } yield response

            case DELETE -> Root / "documents" / DocIdVar(id) =>
                for {
                    service <- ZIO.service[DocumentService]
                    _ <- service.deleteDocument(id)
                    response <- NoContent()
                } yield response
        }

    // Wire everything together
    def run =
        documentRoutes
            .provideSomeLayer[CurrentUser & PermissionService](documentServiceLayer)
            .provideSomeLayer[AuthenticationService](permissionServiceLayer)
            .orDie
```

### Comparison: Role-Based vs. Permission-Based Authorization

**Old Approach (Role-Based - Avoid This):**

```scala
// Hardcoded role checks - inflexible, difficult to manage
def deleteDocument(id: DocId): ZIO[CurrentUser, AppError, Unit] =
    for {
        user <- CurrentUser.use(identity)
        _ <- if (user.roles.contains(UserRole.unsafe("admin")))
                repository.delete(id)
             else
                ZIO.fail(AppError.Forbidden("Only admins can delete"))
    } yield ()
```

**Problems with role-based:**
- Roles hardcoded throughout codebase
- Can't express "user is owner of this document"
- Can't handle complex scenarios like "editors of parent folder can view"
- No way to delegate permissions
- Difficult to audit who has access to what

**New Approach (Permission-Based - Use This):**

```scala
// Relationship-based - flexible, centralized, auditable
def deleteDocument(id: DocId): ZIO[CurrentUser & PermissionService, AppError | AuthenticationError, Unit] =
    val target = PermissionTarget.unsafe("document", id.value)
    Authorization.require(PermissionOp("delete"), target) {
        repository.delete(id)
    }
```

**Benefits of permission-based:**
- Permissions defined centrally in `PermissionConfig`
- Can express fine-grained relationships ("alice is owner of document:123")
- Supports permission inheritance ("owner" implies "editor" and "viewer")
- Can traverse relationships ("user can view if parent folder grants access")
- Easy to audit and query permissions
- Testable with configurable test implementation

### Old "Proposed Architecture" Sections

The sections below were written before discovering the existing systems. They contain useful concepts but should be adapted to work with `BasicProfile` and `CurrentUser` rather than creating new types.

---

**DEPRECATED - Use existing `BasicProfile` instead:**

```scala
// PURPOSE: Define authentication service contract
// PURPOSE: Enable swappable authentication providers

trait AuthenticationService[R]:
    /** Authenticate a request and return user context */
    def authenticate(request: Request[RIO[R, *]]): IO[AuthError, UserContext]

    /** Get callback routes for OAuth flows (if applicable) */
    def callbackRoutes: Option[HttpRoutes[RIO[R, *]]]

    /** Get logout routes */
    def logoutRoutes: Option[HttpRoutes[RIO[R, *]]]

enum AuthError:
    case Unauthenticated(message: String)
    case Forbidden(message: String)
    case InvalidToken(message: String)
    case ConfigurationError(message: String)
```

**OIDC Implementation:**

```scala
// PURPOSE: Implement OIDC authentication using Pac4J
// PURPOSE: Provide production-grade authentication

class OidcAuthenticationService[R](
    pac4jSecurity: Pac4jHttpSecurity[RIO[R, *]],
    config: OidcAuthConfig
) extends AuthenticationService[R]:

    override def authenticate(request: Request[RIO[R, *]]): IO[AuthError, UserContext] =
        // Extract profile from Pac4J
        // Map to UserContext
        // Validate claims
        ???

    override def callbackRoutes: Option[HttpRoutes[RIO[R, *]]] =
        Some(pac4jSecurity.route)

    override def logoutRoutes: Option[HttpRoutes[RIO[R, *]]] =
        Some(/* logout routes */)
```

**Test Implementation:**

```scala
// PURPOSE: Implement simple test authentication
// PURPOSE: Enable easy testing with predefined users

case class TestUser(
    id: UserId,
    name: String,
    email: Option[String],
    roles: Set[Role],
    customData: Map[String, String]
)

class TestAuthenticationService[R](
    testUsers: List[TestUser],
    defaultUser: Option[TestUser] = None
) extends AuthenticationService[R]:

    override def authenticate(request: Request[RIO[R, *]]): IO[AuthError, UserContext] =
        request.headers.get(ci"X-Test-User-Id") match
            case Some(userId) =>
                testUsers.find(_.id.value == userId.head.value) match
                    case Some(user) => ZIO.succeed(user.toUserContext)
                    case None => ZIO.fail(AuthError.Unauthenticated(s"Unknown test user: $userId"))
            case None =>
                defaultUser match
                    case Some(user) => ZIO.succeed(user.toUserContext)
                    case None => ZIO.fail(AuthError.Unauthenticated("No test user specified"))

    override def callbackRoutes: Option[HttpRoutes[RIO[R, *]]] =
        Some(testUserPickerUI)

    private def testUserPickerUI: HttpRoutes[RIO[R, *]] =
        HttpRoutes.of {
            case GET -> Root / "test-login" =>
                // Return HTML with user selection dropdown
                ???
            case POST -> Root / "test-login" / userId =>
                // Set X-Test-User-Id header or session cookie
                ???
        }
```

**Configuration-Based Provider Selection:**

```scala
// PURPOSE: Load appropriate authentication service based on environment
// PURPOSE: Ensure type-safe configuration

enum AuthConfig:
    case Oidc(urlBase: String, clientId: String, clientSecret: String, discoveryUri: String)
    case Test(users: List[TestUser], defaultUserId: Option[UserId])

object AuthenticationService:
    def layer[R]: ZLayer[AuthConfig & Pac4jDependencies, Throwable, AuthenticationService[R]] =
        ZLayer.fromZIO {
            for {
                config <- ZIO.service[AuthConfig]
                service <- config match
                    case AuthConfig.Oidc(_, _, _, _) =>
                        ZIO.service[Pac4jDependencies].map { deps =>
                            OidcAuthenticationService(deps.security, config)
                        }
                    case AuthConfig.Test(users, defaultId) =>
                        ZIO.succeed(TestAuthenticationService(users, defaultId.flatMap(id => users.find(_.id == id))))
            } yield service
        }
```

### 2. Universal User Information Model

**Core User Context:**

```scala
// PURPOSE: Define universal user information available to all applications
// PURPOSE: Support extensible custom data per project

/** Persistent unique user identifier */
opaque type UserId = String
object UserId:
    def apply(value: String): UserId = value
    extension (id: UserId) def value: String = id

/** User role for authorization */
opaque type Role = String
object Role:
    def apply(value: String): Role = value
    extension (role: Role) def value: String = role

    // Common roles
    val Admin: Role = Role("admin")
    val User: Role = Role("user")
    val Guest: Role = Role("guest")

/** Universal user context available throughout application */
case class UserContext(
    /** Persistent unique identifier */
    id: UserId,

    /** Display name */
    name: String,

    /** Optional email address */
    email: Option[String],

    /** Set of user roles for authorization */
    roles: Set[Role],

    /** Project-specific custom data */
    customData: Map[String, Json]
):
    /** Check if user has specific role */
    def hasRole(role: Role): Boolean = roles.contains(role)

    /** Check if user has any of the specified roles */
    def hasAnyRole(roles: Set[Role]): Boolean = roles.intersect(this.roles).nonEmpty

    /** Check if user has all of the specified roles */
    def hasAllRoles(roles: Set[Role]): Boolean = roles.subsetOf(this.roles)

    /** Get typed custom data */
    def getCustomData[A: Decoder](key: String): Option[A] =
        customData.get(key).flatMap(_.as[A].toOption)
```

**Project-Specific Extensions:**

```scala
// Example: HR application with organization-specific data
case class HRUserContext(
    base: UserContext,
    department: String,
    position: String,
    employeeId: String,
    manager: Option[UserId]
):
    export base.{id, name, email, roles, hasRole, hasAnyRole, hasAllRoles}

object HRUserContext:
    def fromUserContext(ctx: UserContext): Option[HRUserContext] =
        for {
            dept <- ctx.getCustomData[String]("department")
            pos <- ctx.getCustomData[String]("position")
            empId <- ctx.getCustomData[String]("employeeId")
            mgr = ctx.getCustomData[UserId]("managerId")
        } yield HRUserContext(ctx, dept, pos, empId, mgr)
```

### 3. ZIO-Based User Context Access

**Environment-Based Context:**

```scala
// PURPOSE: Provide user context via ZIO environment
// PURPOSE: Enable type-safe access to authenticated user throughout application

/** ZIO environment providing authenticated user context */
type Principal[U] = U

object Principal:
    /** Get current user context */
    def get[U]: URIO[Principal[U], U] = ZIO.service[U]

    /** Provide user context to effect */
    def provide[R, E, A, U](user: U)(effect: ZIO[R & Principal[U], E, A]): ZIO[R, E, A] =
        effect.provideSomeEnvironment[R](_.add(user))
```

**Higher-Order Access Control Effects:**

```scala
// PURPOSE: Declarative, composable authorization checks
// PURPOSE: Type-safe protection of business logic

object Auth:
    /** Require authenticated user */
    def withUser[R, E, A, U](
        f: U => ZIO[R, E, A]
    ): ZIO[R & Principal[U], E, A] =
        Principal.get[U].flatMap(f)

    /** Require specific role */
    def requireRole[R, E, U <: UserContext](
        role: Role
    )(effect: ZIO[R & Principal[U], E, *]): ZIO[R & Principal[U], E | AuthError, Unit] =
        Principal.get[U].flatMap { user =>
            if (user.hasRole(role)) effect
            else ZIO.fail(AuthError.Forbidden(s"Required role: ${role.value}"))
        }

    /** Require any of the specified roles */
    def requireAnyRole[R, E, U <: UserContext](
        roles: Set[Role]
    )(effect: ZIO[R & Principal[U], E, *]): ZIO[R & Principal[U], E | AuthError, Unit] =
        Principal.get[U].flatMap { user =>
            if (user.hasAnyRole(roles)) effect
            else ZIO.fail(AuthError.Forbidden(s"Required one of roles: ${roles.map(_.value).mkString(", ")}"))
        }

    /** Require all of the specified roles */
    def requireAllRoles[R, E, U <: UserContext](
        roles: Set[Role]
    )(effect: ZIO[R & Principal[U], E, *]): ZIO[R & Principal[U], E | AuthError, Unit] =
        Principal.get[U].flatMap { user =>
            if (user.hasAllRoles(roles)) effect
            else ZIO.fail(AuthError.Forbidden(s"Required all roles: ${roles.map(_.value).mkString(", ")}"))
        }

    /** Custom authorization predicate */
    def require[R, E, U](
        predicate: U => Boolean,
        errorMessage: => String
    )(effect: ZIO[R & Principal[U], E, *]): ZIO[R & Principal[U], E | AuthError, Unit] =
        Principal.get[U].flatMap { user =>
            if (predicate(user)) effect
            else ZIO.fail(AuthError.Forbidden(errorMessage))
        }
```

**Usage Example:**

```scala
// Service layer with declarative access control
class DocumentService[R](repo: DocumentRepository):

    /** Get document - any authenticated user */
    def getDocument(id: DocId): ZIO[R & Principal[UserContext], AppError, Document] =
        Auth.withUser { user =>
            repo.findById(id).flatMap {
                case Some(doc) => ZIO.succeed(doc)
                case None => ZIO.fail(AppError.NotFound(s"Document $id"))
            }
        }

    /** Delete document - admin only */
    def deleteDocument(id: DocId): ZIO[R & Principal[UserContext], AppError | AuthError, Unit] =
        Auth.requireRole(Role.Admin) {
            repo.delete(id)
        }

    /** Update document - owner or admin */
    def updateDocument(id: DocId, content: String): ZIO[R & Principal[UserContext], AppError | AuthError, Unit] =
        for {
            user <- Principal.get[UserContext]
            doc <- repo.findById(id).someOrFail(AppError.NotFound(s"Document $id"))
            _ <- Auth.require[R, AppError, UserContext](
                u => u.id == doc.ownerId || u.hasRole(Role.Admin),
                "Only document owner or admin can update"
            ) {
                repo.update(id, content)
            }
        } yield ()
```

**HTTP4S Integration:**

```scala
// PURPOSE: Integrate authentication with HTTP4S routes
// PURPOSE: Provide user context to endpoint handlers

class AuthenticatedModule[R, U <: UserContext](
    authService: AuthenticationService[R]
) extends ZIOWebModule[R]:

    def authedRoutes(routes: AuthedRoutes[U, RIO[R, *]]): HttpRoutes[RIO[R, *]] =
        val middleware: AuthMiddleware[RIO[R, *], U] = AuthMiddleware { request =>
            authService.authenticate(request).either.map {
                case Right(user) => request.context.asInstanceOf[U]  // Safe if properly configured
                case Left(error) => throw new Exception(error.toString)  // Or handle appropriately
            }
        }

        middleware(routes)
```

### 4. Complete Integration Example

```scala
// Application setup
object MyApp extends ZIOAppDefault:

    // Configuration
    val configLayer = ZLayer.fromZIO {
        // Load from environment, config file, etc.
        ZIO.attempt(AuthConfig.Test(
            users = List(
                TestUser(UserId("admin"), "Admin User", Some("admin@test.com"), Set(Role.Admin), Map.empty),
                TestUser(UserId("user1"), "Regular User", Some("user@test.com"), Set(Role.User), Map.empty)
            ),
            defaultUserId = Some(UserId("user1"))
        ))
    }

    // Authentication service layer
    val authLayer = configLayer >>> AuthenticationService.layer[Any]

    // Application routes
    def routes[R: Tag](using auth: AuthenticationService[R]): HttpRoutes[RIO[R, *]] =
        val publicRoutes = HttpRoutes.of[RIO[R, *]] {
            case GET -> Root / "health" => Ok("healthy")
        }

        val authenticatedRoutes = AuthedRoutes.of[UserContext, RIO[R, *]] {
            case GET -> Root / "profile" as user =>
                Ok(s"Hello ${user.name}")

            case GET -> Root / "admin" as user =>
                Auth.requireRole(Role.Admin) {
                    Ok("Admin area")
                }.catchAll {
                    case AuthError.Forbidden(msg) => Forbidden(msg)
                    case _ => InternalServerError("Auth error")
                }
        }

        publicRoutes <+> auth.callbackRoutes.getOrElse(HttpRoutes.empty) <+>
            authenticatedModule.authedRoutes(authenticatedRoutes)

    def run = ???  // Wire everything together
```

## Efficient Authorization-Aware Repository Queries

**The Problem:** Naive filtering loads all objects from the repository and checks permissions on each, which is extremely inefficient:

```scala
// ANTI-PATTERN - Don't do this!
def listDocuments: ZIO[CurrentUser & PermissionService & Repository, AppError, List[Document]] =
    for {
        allDocs <- repository.findAll  // Load everything!
        viewable <- Authorization.filterAllowed(PermissionOp("view"), allDocs)  // Check each one!
    } yield viewable
```

**Problems:**
- Loads all data from database
- Makes N permission checks for N documents
- Doesn't work with pagination
- Terrible performance at scale

### Solution 1: Two-Phase Query (Reverse Lookup)

The most common pattern: First get allowed object IDs from the permission service, then query the repository.

**Implementation:**

```scala
// PURPOSE: Efficiently list documents user can view
// PURPOSE: Uses reverse lookup to avoid loading all documents

trait PermissionService:
    def isAllowed(subj: Option[UserInfo], action: PermissionOp, obj: PermissionTarget): UIO[Boolean]

    /** NEW: List all objects of a given namespace that user can access with action */
    def listAllowed(
        subj: UserInfo,
        action: PermissionOp,
        namespace: String
    ): UIO[Set[String]]  // Returns object IDs

class InMemoryPermissionService(
    tuples: Ref[Set[RelationTuple]],
    config: PermissionConfig
) extends PermissionService:

    override def listAllowed(
        subj: UserInfo,
        action: PermissionOp,
        namespace: String
    ): UIO[Set[String]] =
        for {
            allTuples <- tuples.get
            // Direct permissions
            directIds = allTuples.collect {
                case RelationTuple(userId, rel, target)
                    if userId == subj.subjectId &&
                       rel == action.value &&
                       target.namespace == namespace =>
                    target.value
            }
            // Permissions via implications (e.g., owner implies viewer)
            impliedBy = config.getImpliedRelations(namespace, action.value)
            impliedIds = allTuples.collect {
                case RelationTuple(userId, rel, target)
                    if userId == subj.subjectId &&
                       impliedBy.contains(rel) &&
                       target.namespace == namespace =>
                    target.value
            }
        } yield directIds ++ impliedIds

// Usage in service
class DocumentService(repository: DocumentRepository):

    def listDocuments(
        page: Int,
        pageSize: Int
    ): ZIO[CurrentUser & PermissionService, AppError, Page[Document]] =
        for {
            user <- CurrentUser.use(identity)
            permSvc <- ZIO.service[PermissionService]
            // Phase 1: Get allowed document IDs
            allowedIds <- permSvc.listAllowed(user, PermissionOp("view"), "document")
            // Phase 2: Query repository with filter
            docs <- repository.findByIds(allowedIds, page, pageSize)
        } yield docs
```

**Advantages:**
- Single permission query instead of N queries
- Repository query uses indexed WHERE clause
- Works with pagination
- Efficient at any scale

**Database Query Pattern:**
```sql
-- Phase 1: Get allowed IDs from permission store
SELECT object_id FROM permissions
WHERE user_id = ? AND namespace = 'document' AND action IN ('view', 'edit', 'owner')

-- Phase 2: Query main table
SELECT * FROM documents
WHERE id IN (?, ?, ?, ...)
ORDER BY created_at DESC
LIMIT ? OFFSET ?
```

### Solution 2: Query Pushdown

Push permission filtering directly into the database query for maximum efficiency.

**Simple Case - Direct Ownership:**

```scala
// PURPOSE: Filter by owner directly in database query
// PURPOSE: Avoid permission service entirely for simple cases

trait DocumentRepository:
    def findByOwner(ownerId: UserId, page: Int, pageSize: Int): IO[RepositoryError, Page[Document]]

class DocumentService(repository: DocumentRepository):

    def listMyDocuments(
        page: Int,
        pageSize: Int
    ): ZIO[CurrentUser, AppError, Page[Document]] =
        for {
            user <- CurrentUser.use(identity)
            docs <- repository.findByOwner(user.subjectId, page, pageSize)
        } yield docs
```

**SQL:**
```sql
SELECT * FROM documents
WHERE owner_id = ?
ORDER BY created_at DESC
LIMIT ? OFFSET ?
```

**Complex Case - Join with Permissions:**

```scala
// PURPOSE: Join document table with permissions table in single query
// PURPOSE: Maximum efficiency for permission-aware listing

case class PermissionQuery(
    userId: UserId,
    namespace: String,
    actions: Set[String]
)

trait DocumentRepository:
    def findWithPermissions(
        query: PermissionQuery,
        page: Int,
        pageSize: Int
    ): IO[RepositoryError, Page[Document]]

// SQL implementation
"""
SELECT DISTINCT d.*
FROM documents d
INNER JOIN permissions p ON p.object_id = d.id::text
WHERE p.user_id = ?
  AND p.namespace = 'document'
  AND p.action IN ('view', 'edit', 'owner')
ORDER BY d.created_at DESC
LIMIT ? OFFSET ?
"""
```

**Advantages:**
- Single database query
- Leverages database indexes
- Optimal performance
- Natural pagination support

**Disadvantages:**
- Requires permission data in same database
- Less flexible for complex permission rules
- Harder to maintain consistency

### Solution 3: Permission Materialization

Pre-compute permission sets periodically for read-heavy workloads.

**Implementation:**

```scala
// PURPOSE: Materialized view of user permissions for fast queries
// PURPOSE: Trade-off between freshness and performance

case class MaterializedPermission(
    userId: UserId,
    objectType: String,
    objectId: String,
    action: String,
    computedAt: Instant
)

trait MaterializedPermissionStore:
    /** Get all object IDs user can access with action */
    def getPermittedIds(
        userId: UserId,
        objectType: String,
        action: String
    ): IO[StoreError, Set[String]]

    /** Refresh permissions for a user */
    def refreshUserPermissions(userId: UserId): IO[StoreError, Unit]

    /** Refresh permissions for an object */
    def refreshObjectPermissions(objectType: String, objectId: String): IO[StoreError, Unit]

// Background job to refresh materialized permissions
class PermissionMaterializationJob(
    permissionService: PermissionService,
    store: MaterializedPermissionStore,
    userRepository: UserRepository
):

    def refreshAll: ZIO[Any, AppError, Unit] =
        for {
            users <- userRepository.findAll
            _ <- ZIO.foreachPar(users)(refreshUser)
        } yield ()

    private def refreshUser(user: UserInfo): ZIO[Any, AppError, Unit] =
        store.refreshUserPermissions(user.subjectId)

// Usage with stale-while-revalidate pattern
class DocumentService(
    repository: DocumentRepository,
    materializedStore: MaterializedPermissionStore,
    permissionService: PermissionService
):

    def listDocuments(
        maxStaleness: Duration = 5.minutes
    ): ZIO[CurrentUser, AppError, List[Document]] =
        for {
            user <- CurrentUser.use(identity)
            // Try materialized store first
            allowedIds <- materializedStore.getPermittedIds(
                user.subjectId,
                "document",
                "view"
            ).catchAll { _ =>
                // Fallback to real-time permission check
                permissionService.listAllowed(user, PermissionOp("view"), "document")
            }
            docs <- repository.findByIds(allowedIds)
        } yield docs
```

**Advantages:**
- Very fast reads (pre-computed)
- Works well for static permissions
- Reduces load on permission service

**Disadvantages:**
- Eventual consistency (stale data)
- Storage overhead
- Complexity in maintaining freshness

### Solution 4: Hybrid Approach

Combine multiple strategies based on use case.

```scala
// PURPOSE: Use different strategies for different query patterns
// PURPOSE: Optimize common cases, handle edge cases correctly

class DocumentService(
    repository: DocumentRepository,
    permissionService: PermissionService
):

    /** List my own documents - direct query, no permission check needed */
    def listMyDocuments: ZIO[CurrentUser, AppError, List[Document]] =
        for {
            user <- CurrentUser.use(identity)
            docs <- repository.findByOwner(user.subjectId)
        } yield docs

    /** List public documents - direct query with public flag */
    def listPublicDocuments: ZIO[Any, AppError, List[Document]] =
        repository.findPublic

    /** List shared documents - requires permission lookup */
    def listSharedDocuments: ZIO[CurrentUser & PermissionService, AppError, List[Document]] =
        for {
            user <- CurrentUser.use(identity)
            // Get IDs where user is viewer/editor (not owner)
            allowedIds <- permissionService.listAllowed(user, PermissionOp("view"), "document")
            ownedIds <- repository.findIdsByOwner(user.subjectId)
            sharedIds = allowedIds -- ownedIds.map(_.value)
            docs <- repository.findByIds(sharedIds)
        } yield docs

    /** Search with permissions - two-phase approach */
    def searchDocuments(
        query: String
    ): ZIO[CurrentUser & PermissionService, AppError, List[Document]] =
        for {
            user <- CurrentUser.use(identity)
            // Phase 1: Search without permission filter (or with simple filter)
            searchResults <- repository.search(query, limit = 1000)
            // Phase 2: Filter by permissions in-memory (for smaller result set)
            viewable <- Authorization.filterAllowed(PermissionOp("view"), searchResults)
        } yield viewable
```

### Solution 5: Cursor-Based Streaming with Permission Checks

For cases where you must check permissions on-the-fly (e.g., complex computed permissions).

```scala
// PURPOSE: Stream results and check permissions incrementally
// PURPOSE: Use when other approaches not feasible

class DocumentService(repository: DocumentRepository):

    def streamDocuments(
        pageSize: Int = 20
    ): ZStream[CurrentUser & PermissionService, AppError, Document] =
        ZStream.unwrap {
            for {
                user <- CurrentUser.use(identity)
                permSvc <- ZIO.service[PermissionService]
            } yield {
                // Stream all documents from repository
                repository.streamAll
                    // Filter by permission
                    .filterZIO { doc =>
                        val target = PermissionTarget.unsafe("document", doc.id.value)
                        permSvc.isAllowed(Some(user), PermissionOp("view"), target)
                    }
                    // Take only what we need
                    .take(pageSize)
            }
        }
```

**Advantages:**
- Works with any permission logic
- Memory efficient (streaming)
- Handles computed permissions

**Disadvantages:**
- Can be slow if user has few permissions
- Unpredictable performance
- Difficult to implement proper pagination

### Indexing Strategy for PermissionService

**Required Indexes:**

```scala
// For efficient reverse lookups
case class RelationTuple(
    user: UserId,       // INDEX: For "what can user X access?"
    relation: String,   // INDEX: Combined with user
    target: PermissionTarget  // INDEX: For "who can access object Y?"
)

// Database schema
"""
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    namespace VARCHAR NOT NULL,
    object_id VARCHAR NOT NULL,
    relation VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Forward lookup: who can access this object?
    INDEX idx_object_lookup (namespace, object_id, relation),

    -- Reverse lookup: what can this user access?
    INDEX idx_user_lookup (user_id, namespace, relation),

    -- Prevent duplicates
    UNIQUE INDEX idx_unique_permission (user_id, namespace, object_id, relation)
);
"""
```

### Performance Comparison

| Approach | Query Time | Memory | Consistency | Complexity | Best For |
|----------|-----------|---------|-------------|------------|----------|
| Naive Filter | O(N checks) | High | Perfect | Low | Never use |
| Two-Phase Query | O(1 query + 1 lookup) | Low | Perfect | Medium | General purpose |
| Query Pushdown | O(1 query) | Low | Perfect | High | Same-DB permissions |
| Materialization | O(1 lookup) | High | Eventual | High | Read-heavy |
| Streaming | O(N checks) | Low | Perfect | Medium | Complex rules |
| Hybrid | Varies | Low | Perfect | Medium | Production |

### Recommendations

**For Most Cases (Recommended):**
```scala
// Use two-phase query with reverse lookup
def listDocuments: ZIO[CurrentUser & PermissionService, AppError, Page[Document]] =
    for {
        user <- CurrentUser.use(identity)
        allowedIds <- permissionService.listAllowed(user, PermissionOp("view"), "document")
        docs <- repository.findByIds(allowedIds, page, pageSize)
    } yield docs
```

**For Simple Ownership:**
```scala
// Direct query, no permission service needed
def listMyDocuments: ZIO[CurrentUser, AppError, List[Document]] =
    repository.findByOwner(currentUser.subjectId)
```

**For Read-Heavy Workloads:**
```scala
// Materialized permissions with background refresh
def listDocuments: ZIO[CurrentUser, AppError, List[Document]] =
    materializedStore.getPermittedIds(user.subjectId, "document", "view")
        .flatMap(ids => repository.findByIds(ids))
```

**For Complex Rules:**
```scala
// Hybrid: simple cases optimized, complex cases correct
def listDocuments: ZIO[CurrentUser & PermissionService, AppError, List[Document]] =
    // Fast path for owner
    if (query.onlyMine) repository.findByOwner(user.subjectId)
    // Permission-aware path for shared
    else twoPhaseQuery(user, "view", "document")
```

## Migration Strategy

### Phase 1: Implement PermissionService
1. **Implement `InMemoryPermissionService`** - For testing and simple deployments
   - Add `RelationTuple` case class
   - Implement permission checking with computed relations
   - Create management API for adding/removing permissions
2. **Create `PermissionConfig`** - Define namespace rules and permission implications
3. **Add test implementation** - `AlwaysAllowPermissionService` and `ConfigurableTestPermissionService`
4. **Document permission model** - How to define namespaces, relations, and implications

### Phase 2: Integrate Pac4J with AuthenticationService
1. **Create `Pac4jAuthenticationAdapter`** - Bridge existing Pac4J to `AuthenticationService`
2. **Map Pac4J profiles to `BasicProfile`** - Extract claims and roles
3. **Integrate with `CurrentUser`** - Use existing `provideCurrentUser` pattern
4. **Add test authentication** - `TestAuthenticationService` with predefined users

### Phase 3: Build Authorization Guards
1. **Create `Authorization` helper object** - Declarative permission checks
2. **Implement `require`, `withPermission`, `check`, `filterAllowed`** methods
3. **Update service layer** - Replace role-based checks with permission-based
4. **Add middleware integration** - HTTP4S authorization middleware

### Phase 4: Database-Backed Permissions
1. **Design permission storage schema** - Efficient indexing for relation tuples
2. **Implement `PermissionRepository`** - Database operations for permissions
3. **Create `DatabasePermissionService`** - Production implementation
4. **Migration tools** - Convert existing role-based rules to permissions

### Phase 5: Enhanced Authorization
1. **Implement transitive permissions** - Parent/child relationships (e.g., folder → document)
2. **Add permission delegation** - Users granting permissions to others
3. **Audit logging** - Track all permission checks and changes
4. **Admin UI** - Manage permissions and view relationships

## Recommendations

### Immediate Actions (Week 1-2)
1. **Implement `InMemoryPermissionService`** - Get working authorization system
   - Provides testability immediately
   - Can be used for simple deployments
   - Foundation for database implementation
2. **Create `Authorization` helper object** - Declarative permission checking
3. **Build `PermissionConfig` for your domain** - Define namespaces and rules
4. **Implement `TestAuthenticationService`** - Critical for testing without OIDC

### Short-term Goals (Month 1)
1. **Integrate Pac4J with `AuthenticationService`** - Connect existing auth
2. **Migrate one service to use `PermissionService`** - Prove the concept
3. **Add comprehensive tests** - Both authentication and authorization
4. **Document permission model** - How to use and extend the system

### Medium-term Goals (Months 2-3)
1. **Implement `DatabasePermissionService`** - Production-ready persistence
2. **Migration from role-based to permission-based** - Convert existing code
3. **Add permission management API** - CRUD operations for permissions
4. **Build permission admin UI** - For non-technical users

### Long-term Considerations (Months 4+)
1. **Advanced relationship traversal** - Transitive permissions, group memberships
2. **Integration with external systems** - SpiceDB, OpenFGA, or other Zanzibar implementations
3. **Performance optimization** - Caching, indexed queries, permission compilation
4. **Audit and compliance** - Permission history, access logs, compliance reports

## Conclusion

The proposed architecture leverages the existing Zanzibar-inspired authorization system while addressing all four requirements:

1. **Pluggable authentication** - Extend existing `AuthenticationService` with Pac4J and test implementations
2. **Universal user model** - Use existing `UserInfo`/`BasicProfile` hierarchy (already implemented!)
3. **ZIO-native access** - Use existing `CurrentUser` service (already implemented!)
4. **Relationship-based authorization** - Implement existing `PermissionService` interface with Zanzibar-style ReBAC

**Key Insights:**

1. **We have excellent foundations** - `UserInfo`, `CurrentUser`, `PermissionService` interfaces already exist
2. **Focus on implementation, not design** - The APIs are well-designed; we need working implementations
3. **Zanzibar > Roles** - Permission-based authorization is more flexible and powerful than hardcoded roles
4. **Testability is critical** - Test implementations of both auth and authz enable rapid development

**Migration Path:**

Instead of large refactoring, we can migrate incrementally:
1. Implement `InMemoryPermissionService` (works alongside existing role-based code)
2. Add permission checks to new features (don't touch existing code yet)
3. Gradually migrate existing services from role-based to permission-based
4. Once migration complete, deprecate role-based patterns

This approach minimizes risk, provides immediate value, and builds toward a production-ready Zanzibar-style authorization system that's more powerful and maintainable than traditional role-based access control.

## References

### Authentication
- [HTTP4S Authentication Documentation](https://http4s.org/v1/docs/auth.html)
- [ZIO HTTP Handler Aspects](https://zio-http.netlify.app/reference/aop/handler_aspect)
- [Pac4J Documentation](https://www.pac4j.org/)
- [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [OIDC Token Validation Best Practices](https://curity.io/resources/learn/validating-an-id-token/)
- [OAuth2 Security Best Practices](https://www.slashid.dev/blog/oauth-security/)

### Authorization (Zanzibar)
- [Google Zanzibar Paper](https://research.google/pubs/zanzibar-googles-consistent-global-authorization-system/) - Original research paper
- [Zanzibar Explained by AuthZed](https://authzed.com/learn/google-zanzibar) - Comprehensive explanation
- [WorkOS Zanzibar Guide](https://workos.com/blog/google-zanzibar-authorization) - Practical introduction
- [SpiceDB](https://authzed.com/spicedb) - Open-source Zanzibar implementation
- [OpenFGA](https://openfga.dev/) - Auth0's Zanzibar implementation
- [Ory Keto](https://www.ory.sh/keto/) - Zanzibar-inspired authorization server
