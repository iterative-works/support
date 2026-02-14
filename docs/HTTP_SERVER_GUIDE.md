# HTTP Server & API Guide

This guide documents how iw-support composes http4s, Tapir, security (Pac4j), and Vite support into a complete HTTP server stack. It covers the library's abstractions and how downstream projects use them.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Module System](#module-system)
3. [Tapir Endpoint Modules](#tapir-endpoint-modules)
4. [HTTP Server](#http-server)
5. [Authentication & Security](#authentication--security)
6. [Vite Integration](#vite-integration)
7. [SPA Serving](#spa-serving)
8. [Route Composition Patterns](#route-composition-patterns)
9. [Configuration Reference](#configuration-reference)
10. [Real-World Examples](#real-world-examples)

## Architecture Overview

The HTTP stack is built on these layers, from bottom to top:

```
┌─────────────────────────────────────────────────────────┐
│                    Application Main                      │
│  (composes modules, wires layers, starts server)         │
├──────────────┬──────────────┬───────────────────────────┤
│  Pac4j       │  Tapir API   │  Vite/SPA                 │
│  Security    │  Endpoints   │  Support                  │
├──────────────┴──────────────┴───────────────────────────┤
│              Module Composition Layer                    │
│  (ModuleRegistry, ZIOWebModule, TapirEndpointModule)    │
├─────────────────────────────────────────────────────────┤
│              HttpServer (BlazeHttpServer)                │
│  (http4s routes, WebSocket support)                     │
├─────────────────────────────────────────────────────────┤
│              ZIO Runtime + ZLayer DI                     │
└─────────────────────────────────────────────────────────┘
```

Key design decisions:
- **http4s** provides the HTTP server runtime (via Blaze backend)
- **Tapir** defines type-safe API endpoints with automatic OpenAPI docs
- **Pac4j** handles OIDC/OAuth authentication as http4s middleware
- **ZIO** provides the effect system and dependency injection via ZLayer
- Modules are the unit of composition — each defines routes or endpoints independently

### Key Packages

| Package | Purpose |
|---------|---------|
| `works.iterative.server.http` | Core module traits, HttpServer, ViteSupport, SPAConfig |
| `works.iterative.server.http.tapir` | TapirEndpointModule, TapirWebModuleAdapter |
| `works.iterative.server.http.impl.blaze` | BlazeHttpServer, BlazeServerConfig |
| `works.iterative.server.http.impl.pac4j` | Pac4jHttpSecurity, Pac4jModuleRegistry, Pac4jSecurityConfig |
| `works.iterative.tapir` | CustomTapir, ApiError, AuthApi, BaseUri, `.toApi`/`.apiLogic` extensions |
| `works.iterative.core.auth` | AuthenticationService, CurrentUser, PermissionService, Authorization |

## Module System

The module system provides composable units for defining HTTP routes. There are three levels, from most general to most specific.

### WebFeatureModule[F[_]]

The simplest abstraction — just routes parameterized by an effect type:

```scala
// Source: server/http/src/main/scala/.../WebFeatureModule.scala
trait WebFeatureModule[F[_]]:
    def routes: HttpRoutes[F]

object WebFeatureModule:
    def combineRoutes[F[_]: Monad](modules: WebFeatureModule[F]*): HttpRoutes[F] =
        modules.foldLeft(HttpRoutes.empty[F])(_ <+> _.routes)
```

### ZIOWebModule[R]

ZIO-specific routes with an environment type `R` and environment widening:

```scala
// Source: server/http/src/main/scala/.../ZIOWebModule.scala
trait ZIOWebModule[R] extends WebFeatureModule[[A] =>> RIO[R, A]]:
    type Env = R
    type WebTask[A] = RIO[R, A]

    def widen[RR <: R]: ZIOWebModule[RR]
```

Use `widen` to make a module with a narrow environment compatible with a wider one. For example, a `ZIOWebModule[HealthCheckService]` can be widened to `ZIOWebModule[AppEnv]` where `AppEnv <: HealthCheckService`.

```scala
// Combining modules with different environment requirements
ZIOWebModule.combineRoutes[AppEnv](
    healthModule.widen[AppEnv],
    metricsModule.widen[AppEnv],
    adminModule.widen[AppEnv]
)
```

### AuthedZIOWebModule[R, C]

For routes that need an authenticated user context `C`:

```scala
// Source: server/http/src/main/scala/.../ZIOWebModule.scala
trait AuthedZIOWebModule[R, C]:
    def publicRoutes: HttpRoutes[WebTask] = HttpRoutes.empty
    def authedRoutes: AuthedRoutes[C, WebTask]
```

This separates public routes (no auth needed) from authenticated routes. The `C` type is typically `AuthedUserInfo` — it's provided by authentication middleware.

An `AuthedZIOWebModule` must be wrapped by security middleware to become a `ZIOWebModule`. See [Pac4jModuleRegistry](#pac4jmoduleregistry) for how this works.

### ModuleRegistry[R]

Aggregates multiple `ZIOWebModule` instances:

```scala
// Source: server/http/src/main/scala/.../ModuleRegistry.scala
trait ModuleRegistry[R]:
    def modules: List[ZIOWebModule[R]]
    def routes: HttpRoutes[[A] =>> RIO[R, A]] =
        ZIOWebModule.combineRoutes(modules*)
```

### DslSupport

Mixin trait that provides `Http4sDsl` for http4s route syntax:

```scala
trait DslSupport:
    self: WebModuleTypes =>
    protected val dsl = Http4sDsl[WebTask]
```

## Tapir Endpoint Modules

### TapirEndpointModule[R, C]

Defines Tapir endpoints and their server implementations. Separate from HTTP routes — converted via `TapirWebModuleAdapter`.

```scala
// Source: server/http/src/main/scala/.../tapir/TapirEndpointModule.scala
trait TapirEndpointModule[R, C] extends CustomTapir:
    /** Endpoint descriptors (for OpenAPI docs) */
    def endpoints: List[Endpoint[?, ?, ?, ?, ?]]

    /** Server endpoints with implementation logic */
    def serverEndpoints: List[ZServerEndpoint[R, C]]

    def widen[RR <: R, CC <: C]: TapirEndpointModule[RR, CC]
    def ++(other: TapirEndpointModule[R, C]): TapirEndpointModule[R, C]
```

Type parameters:
- `R` — ZIO environment required by the endpoint implementations
- `C` — Tapir capabilities (typically `Any` or `ZioStreams` or `ZioStreams & WebSockets`)

#### Defining a TapirEndpointModule

```scala
class HealthCheckTapirModule extends TapirEndpointModule[HealthCheckService, Any]:

    val healthCheckEndpoint = endpoint
        .name("Health Check")
        .description("Check system health")
        .tag("Monitoring")
        .get
        .in("health")
        .out(jsonBody[SystemHealth])

    val healthCheckServerEndpoint = healthCheckEndpoint
        .zServerLogic(_ => HealthCheckService.checkHealth())

    override def endpoints = List(healthCheckEndpoint)
    override def serverEndpoints = List(healthCheckServerEndpoint)
```

#### Streaming Endpoints

Use `ZioStreams` as the capability type:

```scala
class StreamingModule extends TapirEndpointModule[MyService, ZioStreams]:

    val streamEndpoint = endpoint
        .get
        .in("export" / path[Long]("id") / "content")
        .out(header[String]("Content-Disposition"))
        .out(streamBody(ZioStreams)(Schema.binary, CodecFormat.Xml()))

    val streamRoute = streamEndpoint
        .zServerLogic { id =>
            MyService.getStream(id).map(stream => ("attachment; filename=export.xml", stream))
        }

    override def endpoints = List(streamEndpoint)
    override def serverEndpoints = List(streamRoute)
```

### TapirWebModuleAdapter

Converts `TapirEndpointModule` instances into `WebFeatureModule` (http4s routes):

```scala
// Source: server/http/src/main/scala/.../tapir/TapirWebModuleAdapter.scala
object TapirWebModuleAdapter:
    def adapt[R, C >: ZioStreams](
        options: Http4sServerOptions[[A] =>> RIO[R, A]] = Http4sServerOptions.default,
        module: TapirEndpointModule[R, C]
    ): WebFeatureModule[[A] =>> RIO[R, A]]

    def combine[R, C >: ZioStreams](
        options: Http4sServerOptions[[A] =>> RIO[R, A]] = Http4sServerOptions.default,
        modules: TapirEndpointModule[R, C]*
    ): WebFeatureModule[[A] =>> RIO[R, A]]
```

Use `adapt` to convert a single module, or `combine` for multiple. The `options` parameter lets you configure server interceptors (logging, metrics, CORS, etc.).

```scala
// With custom server options (e.g., metrics interceptor)
val serverOptions = Http4sServerOptions
    .customiseInterceptors
    .serverLog(myServerLog)
    .metricsInterceptor(metricsInterceptor)
    .options

val routes = TapirWebModuleAdapter.adapt(serverOptions, myTapirModule)
```

### The toApi/apiLogic Pattern

For endpoints that need bearer token authentication and `CurrentUser` injection:

**Step 1: `.toApi[E]`** adds bearer auth security and error output mapping:

```scala
// Source: tapir/shared/src/main/scala/.../CustomTapir.scala
extension [I, O](base: Endpoint[Unit, I, Unit, O, ZioStreams])
    def toApi[E: JsonCodec: Schema]: ApiEndpoint[E, I, O] =
        base
            .securityIn(auth.bearer[AccessToken]())
            .errorOut(
                oneOf[ApiError[E]](
                    oneOfVariant[ApiError.AuthFailure](StatusCode.Unauthorized, jsonBody[...]),
                    oneOfDefaultVariant[ApiError.RequestFailure[E]](StatusCode.BadRequest, ...)
                )
            )
```

**Step 2: `.apiLogic`** binds implementation logic with automatic `CurrentUser` provisioning:

```scala
// Source: tapir/jvm/src/main/scala/.../CustomTapirPlatformSpecific.scala
extension [E, I, O](endpoint: ApiEndpoint[E, I, O])
    def apiLogic[R <: AuthenticationService](
        logic: I => ZIO[R & CurrentUser, E | AuthenticationError, O]
    ): ZServerEndpoint[R, ZioStreams]
```

This:
1. Extracts the bearer token from the request
2. Uses `AuthenticationService.provideCurrentUser` to set up the `CurrentUser` context
3. Maps `AuthenticationError` to `ApiError.AuthFailure` (→ 401)
4. Maps application errors `E` to `ApiError.RequestFailure[E]` (→ 400)

**Complete example:**

```scala
val updateDocument = endpoint
    .put
    .in("documents" / path[String]("id"))
    .in(jsonBody[UpdateRequest])
    .out(jsonBody[Document])
    .toApi[Unit]  // E = Unit means no application-level errors in error output
    .apiLogic { case (id, req) =>
        for
            service <- ZIO.service[DocumentService]
            doc <- service.updateDocument(id, req.title)  // May throw AuthenticationError
        yield doc
    }
```

### ApiError

The error type used by `.toApi`:

```scala
// Source: tapir/shared/src/main/scala/.../ApiError.scala
sealed trait ApiError[+ClientError]
object ApiError:
    case class AuthFailure(error: AuthenticationError) extends ApiError[Nothing]
    case class RequestFailure[ClientError](error: ClientError) extends ApiError[ClientError]
```

### AuthApi

Pre-built endpoint for retrieving the current authenticated user:

```scala
// Source: tapir/jvm/src/main/scala/.../AuthApi.scala
trait AuthApi(ep: AuthenticationEndpoints):
    val currentUser: ZServerEndpoint[AuthenticationService, Any] =
        ep.currentUser.zServerLogic(_ =>
            ZIO.serviceWithZIO[AuthenticationService](_.currentUserInfo)
        )
```

Mix this into your API object:

```scala
object Api extends CustomTapir with AuthApi(ApiEndpoints):
    // currentUser endpoint is available
```

### Http4sCustomTapir[Env]

Combines `CustomTapir` with `ZHttp4sServerInterpreter` for direct endpoint-to-routes conversion:

```scala
// Source: tapir/jvm/src/main/scala/.../Http4sCustomTapir.scala
trait Http4sCustomTapir[Env] extends CustomTapir with ZHttp4sServerInterpreter[Env]
```

Use when you need to convert endpoints to routes inline (without `TapirWebModuleAdapter`):

```scala
val interpreter = new Http4sCustomTapir[Env] {}
val routes: HttpRoutes[RIO[Env, *]] = interpreter.from(serverEndpoints).toRoutes
val wsRoutes: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]] =
    interpreter.fromWebSocket(wsEndpoints).toRoutes
```

## HTTP Server

### HttpServer Trait

The core server interface:

```scala
// Source: server/http/src/main/scala/.../HttpServer.scala
trait HttpServer:
    def build[Env](
        app: WebSocketBuilder2[[A] =>> RIO[Env, A]] => HttpRoutes[[A] =>> RIO[Env, A]]
    ): RIO[Env & Scope, org.http4s.server.Server]

    def serve[Env](
        app: WebSocketBuilder2[[A] =>> RIO[Env, A]] => HttpRoutes[[A] =>> RIO[Env, A]]
    ): URIO[Env, Nothing]
```

- `build` — Creates the server as a scoped resource (returns when server is ready)
- `serve` — Runs the server forever (blocks until termination)

Both accept a function `WebSocketBuilder2 => HttpRoutes` to support WebSocket endpoints.

The companion object provides ZIO accessor methods:

```scala
HttpServer.serve[AppEnv](wsb => allRoutes(wsb))
// or if no WebSocket support needed:
HttpServer.serve[AppEnv](_ => allRoutes)
```

### BlazeHttpServer

The http4s Blaze implementation:

```scala
// Source: server/http/src/main/scala/.../impl/blaze/BlazeHttpServer.scala
class BlazeHttpServer(config: BlazeServerConfig, baseUri: BaseUri) extends HttpServer
```

Features:
- Configurable host/port, response header timeout, idle timeout
- Automatic `baseUri` routing prefix (if configured)
- WebSocket support via `WebSocketBuilder2`

Provide via:

```scala
BlazeHttpServer.layer  // ZLayer that reads BlazeServerConfig and BaseUri from ZIO Config
```

### BaseUri

URI wrapper used for path prefixing and WebSocket URI conversion:

```scala
// Source: tapir/shared/src/main/scala/.../BaseUri.scala
case class BaseUri(value: Option[Uri]):
    def /(s: String): BaseUri          // Append path segment
    def toWSUri: Option[Uri]           // Convert http→ws, https→wss
    def href: String                   // URI string or "#"
    def orRoot: String                 // URI string or "/"
```

Configuration:

```hocon
# Sets baseUri to Some(uri"http://localhost:8080/myapp")
base-uri = "http://localhost:8080/myapp"
# Or leave unset for BaseUri(None) — no path prefix
```

## Authentication & Security

There are two authentication approaches used in practice:

### Approach 1: Pac4j (Session-Based, OIDC)

Used when the server hosts the web UI directly and needs session-based auth via OIDC.

#### Pac4jHttpSecurity[F]

HTTP4s middleware for Pac4j-based authentication:

```scala
// Source: server/http/src/main/scala/.../impl/pac4j/Pac4jHttpSecurity.scala
class Pac4jHttpSecurity[F[_] <: AnyRef: Sync](
    baseUri: BaseUri,
    config: Pac4jSecurityConfig,
    pac4jConfig: Config,        // Pac4j Config object
    dispatcher: Dispatcher[F]
) extends HttpSecurity
```

Key methods:

```scala
// Callback/logout routes — mount at config.callbackBase path
def route: HttpRoutes[F]

// Auth middleware — wraps routes requiring authentication
def secure(
    authorizers: Option[String] = None,
    matchers: Option[String] = None,
    clients: Option[String] = None
): AuthMiddleware[F, List[CommonProfile]]
```

The `secure()` middleware:
1. Checks the session for an authenticated user
2. If not authenticated, redirects to the OIDC provider
3. On return, provides `List[CommonProfile]` to downstream routes
4. Manages session cookies automatically

#### Pac4jModuleRegistry[R, U]

Combines `ModuleRegistry` with Pac4j auth middleware:

```scala
// Source: server/http/src/main/scala/.../impl/pac4j/Pac4jModuleRegistry.scala
trait Pac4jModuleRegistry[R, U] extends ModuleRegistry[R]:
    def pac4jSecurity: Pac4jHttpSecurity[[A] =>> RIO[R, A]]
    def profileToUser(profile: List[CommonProfile]): Option[U]
    def clients: Option[String] = None

    protected def wrapModule(protectedPath: String, module: AuthedZIOWebModule[R, U]): ZIOWebModule[R]
    protected def wrapModules(protectedPath: String, modules: AuthedZIOWebModule[R, U]*): ZIOWebModule[R]
```

This trait:
- Takes `AuthedZIOWebModule` instances (which define `authedRoutes`)
- Wraps them with `pac4jSecurity.secure()` middleware
- Converts `List[CommonProfile]` → your user type `U` via `profileToUser`
- Mounts wrapped routes under `protectedPath`

Example implementation:

```scala
class AppModuleRegistry(
    pac4jSecurity: AppPac4jHttpSecurity,
    assetsModule: AssetsModule,
    spaModule: SPAModule,
    adminModule: AdminModule
) extends Pac4jModuleRegistry[Env, AuthedUserInfo]:

    override def profileToUser(profile: List[CommonProfile]): Option[AuthedUserInfo] =
        profile match
            case (p: OidcProfile) :: _ => Some(mapOidcProfile(p))
            case _ => None

    override def modules: List[ZIOWebModule[Env]] =
        List(
            assetsModule.widen,     // Public — static files, no auth
            wrapModules(            // Protected — all need auth
                "protected",
                spaModule.widen,
                adminModule.widen
            )
        )
```

#### Pac4jSecurityConfig

```scala
// Source: server/http/src/main/scala/.../impl/pac4j/Pac4jSecurityConfig.scala
case class Pac4jSecurityConfig(
    urlBase: String,                        // e.g., "https://example.com"
    callbackBase: String,                   // e.g., "/security"
    defaultUrl: Option[String],             // redirect after login
    logoutUrl: Option[String],              // redirect after logout
    logoutUrlPattern: Option[String],
    sessionSecret: String,
    client: OidcClientConfig,               // primary OIDC provider
    clients: Map[String, OidcClientConfig]  // additional named providers
)

case class OidcClientConfig(
    clientId: String,
    clientSecret: String,
    discoveryURI: String
)
```

### Approach 2: Reverse Proxy Headers

Used when authentication is handled by a reverse proxy (nginx, Traefik, etc.) and the server just reads identity headers.

In this case, there is no Pac4j. The server extracts user info from headers like:
- `X-User-ID` — user identifier
- `X-User-Email` — email claim
- `X-Auth-Method` — authentication method
- `X-User-Groups` — group memberships

This is an application-level pattern, not a library abstraction — each project implements it as needed (e.g., in a custom `ServerLog` or middleware).

### Approach 3: Bearer Token API Auth (via toApi/apiLogic)

For stateless API authentication using bearer tokens. This is the Tapir `.toApi` + `.apiLogic` pattern described in [the toApi/apiLogic section](#the-toapiapilogic-pattern).

The `AuthenticationService` resolves the token to a `CurrentUser`:

```scala
trait AuthenticationService:
    def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit]
    def currentUserInfo: UIO[Option[AuthedUserInfo]]
    def provideCurrentUser[R, E, A](zio: ZIO[R & CurrentUser, E, A]): ZIO[R, E | AuthenticationError, A]
```

### AuthErrorHandler

Maps `AuthenticationError` to HTTP responses:

```scala
// Source: server/http/src/main/scala/.../AuthErrorHandler.scala
object AuthErrorHandler:
    def toResponse(error: AuthenticationError): Response[IO]
    // Unauthenticated → 401 + {"error": "Unauthenticated", "messageId": "..."}
    // Forbidden → 403 + {"error": "Forbidden", "resourceType": "...", "action": "..."}
    // InvalidCredentials → 401
    // TokenExpired → 401
    // InvalidToken → 401
```

### Authorization

For fine-grained permission checking, see [AUTHORIZATION_GUIDE.md](AUTHORIZATION_GUIDE.md). The key patterns are:

- `Authorization.require(op, target)(effect)` — Guard an effect, fail with `Forbidden` if denied
- `Authorization.filterAllowed(op, items)(targetFn)` — Filter a collection by permission
- `Authorization.check(op, target)` — Boolean permission check

## Vite Integration

### ScalatagsViteSupport

Generates HTML `<link>` and `<script>` tags for Vite-built assets:

```scala
// Source: server/http/src/main/scala/.../ViteSupport.scala
class ScalatagsViteSupport(
    entrypoints: PartialFunction[String, ScalatagsViteSupport.Entries]
) extends ViteSupport[Seq[Text.Modifier]]:
    def mainCss: Seq[Text.Modifier]              // CSS link tags for "main.css" entrypoint
    def preambleFor(entrypoint: String): Seq[Text.Modifier]  // All tags for an entrypoint
```

`Entries` contains the resolved asset URLs:

```scala
case class Entries(
    stylesheet: List[String] = Nil,   // CSS files
    module: List[String] = Nil,       // JS module files
    preload: List[String] = Nil       // Preloaded modules
):
    val preamble: Seq[Text.Modifier] = stylesheets ++ modules ++ preloads
    def withBaseUri(baseUri: BaseUri): Entries
```

### Three Operation Modes

Controlled by configuration (priority order):

| Config Key | Mode | When to Use |
|-----------|------|-------------|
| `VITE_BASE` | **Development** | Vite dev server running at base URL, enables HMR |
| `VITE_FILE` | **File manifest** | Reads `manifest.json` from filesystem (staging) |
| `VITE_RESOURCE` | **Resource manifest** | Reads `manifest.json` from classpath (production) |

**Development mode** generates:
```html
<script type="module" src="http://localhost:5173/@vite/client"></script>
<script type="module" src="http://localhost:5173/main.js"></script>
```

**Production mode** reads Vite's `manifest.json` and resolves CSS, JS chunks, and preloads:
```html
<link rel="stylesheet" href="/assets/main-abc123.css">
<script type="module" src="/assets/main-def456.js"></script>
<link rel="modulepreload" href="/assets/vendor-ghi789.js">
```

### Using in HTML Templates

```scala
class MyAppShell(viteSupport: ScalatagsViteSupport):
    def wrapSPA(pageTitle: String): Frag =
        html(
            head(
                meta(charset := "utf-8"),
                scalatags.Text.tags2.title(pageTitle),
                viteSupport.mainCss,                    // CSS links
                viteSupport.preambleFor("main.js")      // JS modules + preloads
            ),
            body(
                div(id := "app")                        // ScalaJS/Laminar mount point
            )
        )
```

### Multiple Vite Builds

For applications serving multiple SPAs from different Vite builds, create separate `ScalatagsViteSupport` instances with different config prefixes:

```scala
// Main app Vite support (reads from VITE_BASE, VITE_FILE, or VITE_RESOURCE)
val mainVite = ScalatagsViteSupport.layer

// Secondary app (reads from ZADATEL_VITE_BASE, etc.)
val secondaryVite = ZLayer.scoped {
    for
        config <- ZIO.config(ScalatagsViteSupport.config.nested("zadatel"))
        baseUri <- ZIO.config(BaseUri.config.nested("zadatel"))
        entries <- ScalatagsViteSupport.entriesFor(config, baseUri)
    yield ScalatagsViteSupport(entries)
}
```

## SPA Serving

### SPAConfig

Configuration for serving a single-page application:

```scala
// Source: server/http/src/main/scala/.../SPAConfig.scala
case class SPAConfig(
    appPath: String = "app",          // Path serving index.html (catch-all)
    appIndex: String = "index.html",  // Index filename
    filePath: Option[String] = None,  // Filesystem path (for dev/staging)
    resourcePath: String = "app"      // Classpath resource path (for production)
)
```

### SPAEndpoints

Creates Tapir endpoints for serving the SPA:

```scala
// Source: server/http/src/main/scala/.../SPAEndpoints.scala
class SPAEndpoints[Env](config: SPAConfig):
    val serverEndpoints: List[ZServerEndpoint[Env, Any]]
```

This creates two endpoints:
1. `GET /{appPath}` — Always serves `index.html` (SPA catch-all)
2. `GET /*` — Serves static files from the configured directory

**Important:** SPA endpoints have a catch-all route, so they should be the **last** routes in the composition.

Usage:

```scala
val spaConfig = SPAConfig(appPath = "app", resourcePath = "app")
val spaEndpoints = SPAEndpoints[Env](spaConfig)

// Convert to routes and mount
val interpreter = new Http4sCustomTapir[Env] {}
val spaRoutes = interpreter.from(spaEndpoints.serverEndpoints).toRoutes
```

## Route Composition Patterns

### Pattern 1: Pure Tapir (Simpler Projects)

For API-only servers or simpler applications:

```scala
// Define endpoint modules
val healthModule = new HealthCheckTapirModule
val apiModule = new MyApiTapirModule

// Combine and adapt
val allEndpoints = TapirEndpointModule.combine(
    healthModule.widen,
    apiModule.widen
)
val routes = TapirWebModuleAdapter.adapt(Http4sServerOptions.default, allEndpoints)

// Serve
HttpServer.serve[AppEnv](_ => routes.routes)
```

### Pattern 2: Tapir + Pac4j + Modules (Full-Featured)

For applications with authenticated UI, public API, and SPAs:

```scala
def setupRoutes(
    pac4jSecurity: Pac4jHttpSecurity[AppTask],
    spaEndpoints: List[ZServerEndpoint[Env, ZioStreams]],
    apiEndpoints: List[ZServerEndpoint[CurrentUser & Env, ZioStreams]],
    wsEndpoints: List[ZServerEndpoint[Env, ZioStreams & WebSockets]]
): URIO[Env, WebSocketBuilder2[AppTask] => HttpRoutes[AppTask]] =
    val interpreter = new Http4sCustomTapir[Env] {}
    val secureInterpreter = new Http4sCustomTapir[Env & CurrentUser] {}

    // Convert endpoints to routes
    val spaRoutes = interpreter.from(spaEndpoints).toRoutes
    val apiRoutes = secureInterpreter.from(apiEndpoints).toRoutes
    val wsRoutes = interpreter.fromWebSocket(wsEndpoints).toRoutes

    // Wrap API routes with authentication
    val securedApiRoutes = provideCurrentUser(apiRoutes)

    ZIO.succeed { wsb =>
        pac4jSecurity.route <+> Router(
            "/api" -> (wsRoutes(wsb) <+> securedApiRoutes),
            "/" -> spaRoutes
        )
    }
```

### Pattern 3: Path-Segmented API Tiers

For APIs with different access levels:

```scala
// Public API — no auth
val publicRoutes = TapirWebModuleAdapter.adapt(options, publicTapirModules)

// Internal API — requires authentication
val internalRoutes = TapirWebModuleAdapter.adapt(options, internalTapirModules)

// Admin API — requires admin role
val adminRoutes = TapirWebModuleAdapter.adapt(options, adminTapirModules)

// Compose with OpenAPI docs per tier
val combinedRoutes = Router(
    "/public/api/v1" -> (publicRoutes.routes <+> publicApiDocs.routes),
    "/internal/api/v1" -> (internalRoutes.routes <+> internalApiDocs.routes),
    "/admin" -> (adminRoutes.routes <+> adminApiDocs.routes)
)
```

### OpenAPI Documentation

Generate Swagger UI from Tapir endpoints:

```scala
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter

val swaggerEndpoints = SwaggerInterpreter()
    .fromEndpoints[RIO[R, *]](
        endpointList,
        Info("My API", "1.0.0", description = Some("API documentation"))
    )

val docsRoutes = ZHttp4sServerInterpreter().from(swaggerEndpoints).toRoutes
```

### The CurrentUser Injection Pattern

When using Pac4j with Tapir API endpoints, you need to bridge between Pac4j's `CommonProfile` and your `CurrentUser` context:

```scala
def provideCurrentUser(routes: HttpRoutes[SecuredTask]): HttpRoutes[AppTask] =
    // 1. Pac4j provides List[CommonProfile]
    // 2. Convert to AuthedUserInfo
    // 3. Create ZEnvironment(CurrentUser(...))
    // 4. Use FunctionK natural transformations to add/remove CurrentUser from environment

    def secureRoutes: AuthedRoutes[AuthedUserInfo, AppTask] =
        Kleisli { ctx =>
            val userEnv = ZEnvironment(CurrentUser(ctx.context.profile))
            val eliminate: SecuredTask ~> AppTask =
                new FunctionK[SecuredTask, AppTask]:
                    override def apply[A](fa: SecuredTask[A]): AppTask[A] =
                        fa.provideSomeEnvironment[Env](env => env ++ userEnv)
            routes.run(ctx.req.mapK(widen)).map(_.mapK(eliminate)).mapK(eliminate)
        }

    pac4jSecurity.secure(clients = Some("OidcClient")).compose(
        profileListToAuthedUserInfo
    )(secureRoutes)
```

## Configuration Reference

All configuration uses ZIO Config and can be provided via environment variables, system properties, or HOCON files.

### BlazeServerConfig

```hocon
blaze {
    host = "0.0.0.0"       # default: "localhost"
    port = 8080             # default: 8080
    responseHeaderTimeout = 30s   # default: http4s default
    idleTimeout = 60s             # default: http4s default
}
```

### BaseUri

```hocon
# Simple string — sets the base URI for all routes and API docs
base-uri = "http://localhost:8080"
# Or leave unset for no prefix
```

### Pac4jSecurityConfig

```hocon
security {
    urlbase = "https://example.com"
    callbackbase = "/security"
    defaulturl = "https://example.com/app"
    logouturl = "/login"
    logouturlpattern = "/security/logout"
    sessionsecret = "your-32-char-secret-here"
    client {
        id = "your-oidc-client-id"
        secret = "your-oidc-client-secret"
        discoveryuri = "https://keycloak.example.com/realms/myrealm/.well-known/openid-configuration"
    }
    # Additional named clients (optional)
    clients {
        KLIENT {
            id = "another-client-id"
            secret = "another-secret"
            discoveryuri = "https://..."
        }
    }
}
```

### SPAConfig

```hocon
spa {
    apppath = "app"               # default: "app"
    appindex = "index.html"       # default: "index.html"
    filepath = "/var/www/dist"    # optional — filesystem path
    resourcepath = "app"          # default: "app" — classpath path
}
```

### ViteConfig

```hocon
vite {
    # Set exactly ONE of these (checked in priority order):
    base = "http://localhost:5173/"    # Dev mode — Vite dev server URL
    file = "/var/www/dist/.vite/manifest.json"   # File manifest
    resource = ".vite/manifest.json"             # Classpath manifest
}
```

## Real-World Examples

### Minimal API Server (xml-rozhrani style)

```scala
object Main extends ZIOAppDefault:
    override def run =
        val program = for
            config <- ZIO.config(SPAConfig.config)
            spaEndpoints = SPAEndpoints[AppEnv](config)
            registry <- ZIO.service[ModuleRegistry[AppEnv]]
            routes = registry.routes
            _ <- HttpServer.serve[AppEnv](_ => routes)
        yield ()

        program.provideSome[Scope](
            BlazeHttpServer.layer,
            ScalatagsViteSupport.layer,
            // ... service layers
        )
```

### Full-Featured Server (medeca-modul-poptavky style)

```scala
object Main extends ZIOAppDefault:
    type Env = Api.ApiEnv
    type AppTask[A] = RIO[Env, A]
    type SecuredTask[A] = RIO[Env & CurrentUser, A]

    override def run =
        for
            config <- ZIO.config(SPAConfig.config)
            pac4jSecurity <- ZIO.service[AppPac4jHttpSecurity]
            spaEndpoints = SPAEndpoints[Env](config)
            routes <- setupRoutes(
                pac4jSecurity,
                spaEndpoints.serverEndpoints,
                Api.endpoints,       // Authenticated API endpoints
                Api.wsEndpoints      // WebSocket endpoints
            )
            _ <- HttpServer.serve(routes)
        yield ()
    .provideSome[Scope](
        BlazeHttpServer.layer,
        AppPac4jHttpSecurity.layer,
        AuthenticationService.layer,
        ScalatagsViteSupport.layer,
        // ... all service and repository layers
    )
```

### Defining a Complete API Object

```scala
object Api extends CustomTapir
    with AuthApi(ApiEndpoints)               // GET /user/me
    with FileApi(ApiEndpoints.file)          // File upload/download
    with MyDomainApi(ApiEndpoints):          // Domain-specific endpoints

    type ApiEnv = MyService & OtherService & AuthenticationService & PermissionService

    val endpoints: List[ZServerEndpoint[CurrentUser & ApiEnv, ZioStreams]] =
        List(
            currentUser.widen,
            domainLoad.widen,
            domainUpdate.widen
        )

    val wsEndpoints: List[ZServerEndpoint[ApiEnv, ZioStreams & WebSockets]] =
        List(domainUpdates.widen)
```

### Providing ZLayer Stack

```scala
val appLayers = ZLayer.make[AppEnv & HttpServer](
    // Infrastructure
    BlazeHttpServer.layer,
    ScalatagsViteSupport.layer,

    // Authentication
    AppPac4jHttpSecurity.layer,
    AuthenticationService.layer,
    PermissionService.layer,

    // Domain services
    MyService.layer,
    OtherService.layer,

    // Repositories
    MyRepository.layer,
    DatabaseSupport.layer
)
```
