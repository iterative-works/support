// PURPOSE: Validated configuration for Pac4j-based OIDC authentication.
// PURPOSE: Composes the OIDC callback URL and cookie path; fails at config-load on misuse.
package works.iterative.server.http
package impl.pac4j

import zio.*

/** Configuration for a single OIDC client. */
case class OidcClientConfig(
    clientId: String,
    clientSecret: String,
    discoveryURI: String
)

/** Configuration for the Pac4j security middleware.
  *
  * Field contract — all values are validated by [[Pac4jSecurityConfig.config]] at load time:
  *
  *   - `urlBase`: absolute origin of the BFF as the browser sees it, e.g. `"http://localhost:8090"`.
  *     Must be scheme + host (+ optional port) with **no** path, query, fragment, or trailing slash.
  *   - `callbackBase`: path prefix where the auth routes are mounted, e.g. `"/auth/oidc"`. Must be
  *     empty (auth at origin root) or start with `/` and **not** end with `/`. No scheme/host. The
  *     OIDC callback URL is composed as [[callbackUrl]] = `urlBase + callbackBase + "/callback"`.
  *   - `cookiePath`: cookie `Path` attribute for the session and CSRF cookies. Defaults to `"/"`.
  *     Must start with `/`.
  *   - `sessionSecret`: HMAC secret used to encrypt session cookies. Generate with
  *     `openssl rand -hex 32`.
  *   - `defaultUrl` / `logoutUrl` / `logoutUrlPattern`: optional overrides for post-login,
  *     post-logout, and logout-URL allowlist patterns.
  *   - `client` / `clients`: OIDC client configuration. `client` is the primary; `clients` is a
  *     named map for additional clients.
  *
  * The [[Pac4jSecurityConfig.config]] descriptor enforces these rules and rejects misconfigured
  * input with [[zio.Config.Error.InvalidData]] at startup, so a malformed URL never reaches the
  * IdP.
  */
case class Pac4jSecurityConfig(
    urlBase: String,
    callbackBase: String,
    defaultUrl: Option[String],
    logoutUrl: Option[String],
    logoutUrlPattern: Option[String],
    sessionSecret: String,
    cookiePath: Option[String],
    client: OidcClientConfig,
    clients: Map[String, OidcClientConfig]
):
    /** Absolute callback URL the OIDC client advertises to the IdP as `redirect_uri`.
      *
      * The Auth0 application's "Allowed Callback URLs" list must contain this exact value.
      */
    def callbackUrl: String = s"$urlBase$callbackBase/callback"

    /** Resolved `Path` attribute for session and CSRF cookies. */
    def resolvedCookiePath: String = cookiePath.getOrElse("/")
end Pac4jSecurityConfig

object Pac4jSecurityConfig:

    /** scheme + host (+ optional port). No path, query, fragment, or trailing slash. */
    private val UrlBaseRe = """^https?://[^/?#]+$""".r

    /** Empty, or `/`-prefixed path with no trailing slash, no scheme, no query/fragment. */
    private val CallbackBaseRe = """^$|^/[^/?#]+(?:/[^/?#]+)*$""".r

    /** Path starting with `/`, no query/fragment. */
    private val CookiePathRe = """^/[^?#]*$""".r

    import Config.*

    val oidcConfig: Config[OidcClientConfig] =
        (string("id") ++ string("secret") ++ string("discoveryuri"))
            .map(OidcClientConfig.apply)

    given config: Config[Pac4jSecurityConfig] =
        (
            string("urlbase") ++
                string("callbackbase") ++
                string("defaulturl").optional ++
                string("logouturl").optional ++
                string("logouturlpattern").optional ++
                string("sessionsecret") ++
                string("cookiepath").optional ++
                oidcConfig.nested("client") ++
                Config.table(oidcConfig).withDefault(Map.empty).nested("clients")
        ).nested("security").map(Pac4jSecurityConfig.apply).mapOrFail(validate)
    end config

    /** Validates the composed config and surfaces clear [[Config.Error.InvalidData]] on misuse. */
    private[pac4j] def validate(
        cfg: Pac4jSecurityConfig
    ): Either[Config.Error, Pac4jSecurityConfig] =
        for
            _ <- ensureMatches(
                Chunk("security", "urlbase"),
                cfg.urlBase,
                UrlBaseRe,
                s"must be an absolute origin (scheme://host[:port]) with no path, query, fragment, or trailing '/', got: '${cfg.urlBase}'"
            )
            _ <- ensureMatches(
                Chunk("security", "callbackbase"),
                cfg.callbackBase,
                CallbackBaseRe,
                s"must be empty or start with '/' (no trailing '/', no scheme, no query/fragment), got: '${cfg.callbackBase}'"
            )
            _ <- cfg.cookiePath match
                case Some(p) =>
                    ensureMatches(
                        Chunk("security", "cookiepath"),
                        p,
                        CookiePathRe,
                        s"must be a path starting with '/' (no scheme, no query/fragment), got: '$p'"
                    )
                case None => Right(())
            _ <- ensureParses(Chunk("security"), cfg.callbackUrl)
        yield cfg

    private def ensureMatches(
        path: Chunk[String],
        value: String,
        re: scala.util.matching.Regex,
        message: String
    ): Either[Config.Error, Unit] =
        if re.matches(value) then Right(())
        else Left(Config.Error.InvalidData(path, message))

    /** Belt-and-suspenders: composed URL must parse as an absolute, host-bearing URI. */
    private def ensureParses(
        path: Chunk[String],
        composed: String
    ): Either[Config.Error, Unit] =
        try
            val uri = java.net.URI.create(composed)
            if uri.isAbsolute && uri.getHost != null && uri.getRawSchemeSpecificPart != null
            then Right(())
            else
                Left(Config.Error.InvalidData(
                    path,
                    s"composed callback URL is malformed (not absolute, no host, or empty): '$composed'"
                ))
        catch
            case e: IllegalArgumentException =>
                Left(Config.Error.InvalidData(
                    path,
                    s"composed callback URL fails to parse: '$composed' — ${e.getMessage}"
                ))
end Pac4jSecurityConfig
