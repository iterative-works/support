// PURPOSE: ZIO Test spec for Pac4jSecurityConfig validation and URL composition.
// PURPOSE: Locks the URL-composition contract so the redirect_uri can never be malformed again.
package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*

object Pac4jSecurityConfigSpec extends ZIOSpecDefault:

    private val validBase: Map[String, String] = Map(
        "security.urlbase" -> "http://localhost:8090",
        "security.callbackbase" -> "/auth/oidc",
        "security.sessionsecret" -> "0123456789abcdef0123456789abcdef",
        "security.client.id" -> "cid",
        "security.client.secret" -> "csec",
        "security.client.discoveryuri" -> "https://example.test/.well-known/openid-configuration"
    )

    private def load(
        map: Map[String, String]
    ): ZIO[Any, Config.Error, Pac4jSecurityConfig] =
        ConfigProvider.fromMap(map).load(Pac4jSecurityConfig.config)

    private def assertInvalidAt(
        result: Exit[Config.Error, Pac4jSecurityConfig],
        pathSegment: String,
        messageHint: String
    ): TestResult =
        val matched = result match
            case Exit.Failure(cause) =>
                cause.failures.exists:
                    case Config.Error.InvalidData(path, msg) =>
                        path.contains(pathSegment) && msg.contains(messageHint)
                    case _ => false
            case _ => false
        assertTrue(matched)

    def spec = suite("Pac4jSecurityConfig")(
        test("loads a well-formed config and exposes composed callbackUrl") {
            for cfg <- load(validBase)
            yield assertTrue(
                cfg.callbackUrl == "http://localhost:8090/auth/oidc/callback",
                cfg.resolvedCookiePath == "/",
                cfg.urlBase == "http://localhost:8090",
                cfg.callbackBase == "/auth/oidc",
                cfg.client.clientId == "cid"
            )
        },
        test("accepts empty callbackbase (auth routes at origin root)") {
            for cfg <- load(validBase + ("security.callbackbase" -> ""))
            yield assertTrue(
                cfg.callbackUrl == "http://localhost:8090/callback"
            )
        },
        test("accepts explicit cookiepath") {
            for cfg <- load(validBase + ("security.cookiepath" -> "/admin"))
            yield assertTrue(cfg.resolvedCookiePath == "/admin")
        },
        test("rejects urlbase with trailing slash") {
            for r <- load(validBase + ("security.urlbase" -> "http://localhost:8090/")).exit
            yield assertInvalidAt(r, "urlbase", "absolute origin")
        },
        test("rejects urlbase that includes a path") {
            for r <- load(validBase + ("security.urlbase" -> "http://localhost:8090/app")).exit
            yield assertInvalidAt(r, "urlbase", "absolute origin")
        },
        test("rejects urlbase without scheme") {
            for r <- load(validBase + ("security.urlbase" -> "localhost:8090")).exit
            yield assertInvalidAt(r, "urlbase", "absolute origin")
        },
        test("rejects callbackbase missing leading slash") {
            for r <- load(validBase + ("security.callbackbase" -> "auth/oidc")).exit
            yield assertInvalidAt(r, "callbackbase", "start with '/'")
        },
        test("rejects callbackbase with trailing slash") {
            for r <- load(validBase + ("security.callbackbase" -> "/auth/oidc/")).exit
            yield assertInvalidAt(r, "callbackbase", "no trailing")
        },
        test("rejects callbackbase containing scheme") {
            for r <- load(
                validBase + ("security.callbackbase" -> "http://other/auth")
            ).exit
            yield assertInvalidAt(r, "callbackbase", "no scheme")
        },
        test("rejects bare '/' callbackbase (would compose to double slash)") {
            for r <- load(validBase + ("security.callbackbase" -> "/")).exit
            yield assertInvalidAt(r, "callbackbase", "no trailing")
        },
        test("rejects cookiepath missing leading slash") {
            for r <- load(validBase + ("security.cookiepath" -> "admin")).exit
            yield assertInvalidAt(r, "cookiepath", "starting with '/'")
        },
        test("rejects cookiepath with query string") {
            for r <- load(validBase + ("security.cookiepath" -> "/admin?x=1")).exit
            yield assertInvalidAt(r, "cookiepath", "no scheme, no query/fragment")
        },
        test("composed callbackUrl parses as an absolute URI") {
            for cfg <- load(validBase)
            yield
                val uri = java.net.URI.create(cfg.callbackUrl)
                assertTrue(
                    uri.isAbsolute,
                    uri.getHost == "localhost",
                    uri.getPort == 8090,
                    uri.getPath == "/auth/oidc/callback"
                )
        },
        test("https urlBase composes correctly") {
            for cfg <- load(
                validBase + (
                    "security.urlbase" -> "https://app.example.com",
                    "security.callbackbase" -> "/auth/oidc"
                )
            )
            yield assertTrue(
                cfg.callbackUrl == "https://app.example.com/auth/oidc/callback"
            )
        },
        test("missing sessionsecret produces MissingData error") {
            for r <- load(validBase - "security.sessionsecret").exit
            yield
                val matched = r match
                    case Exit.Failure(cause) =>
                        cause.failures.exists:
                            case Config.Error.MissingData(path, _) =>
                                path.exists(_.toLowerCase.contains("sessionsecret"))
                            case _ => false
                    case _ => false
                assertTrue(matched)
        }
    )
end Pac4jSecurityConfigSpec
