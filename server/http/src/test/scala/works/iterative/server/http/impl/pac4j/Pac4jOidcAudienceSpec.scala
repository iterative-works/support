// PURPOSE: Pins the optional `audience` knob on OidcClientConfig and its propagation to Pac4j.
// PURPOSE: Covers (a) config descriptor parses `audience`, (b) factory writes `audience` custom param.

package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.test.*
import org.pac4j.oidc.config.OidcConfiguration
import scala.jdk.CollectionConverters.*

object Pac4jOidcAudienceSpec extends ZIOSpecDefault:

    def spec = suite("OidcClientConfig audience")(

        test("audience is None by default on OidcClientConfig") {
            val cfg = OidcClientConfig("cid", "csec", "https://issuer/.well-known/openid-configuration")
            assertTrue(cfg.audience.isEmpty)
        },

        test("Pac4jSecurityConfig.oidcConfig parses optional audience from config") {
            val provider = ConfigProvider.fromMap(Map(
                "id"           -> "client-1",
                "secret"       -> "shh",
                "discoveryuri" -> "https://issuer/.well-known/openid-configuration",
                "audience"     -> "my-api-audience"
            ))
            for cfg <- provider.load(Pac4jSecurityConfig.oidcConfig)
            yield assertTrue(cfg.audience.contains("my-api-audience"))
        },

        test("Pac4jSecurityConfig.oidcConfig omits audience when not provided") {
            val provider = ConfigProvider.fromMap(Map(
                "id"           -> "client-1",
                "secret"       -> "shh",
                "discoveryuri" -> "https://issuer/.well-known/openid-configuration"
            ))
            for cfg <- provider.load(Pac4jSecurityConfig.oidcConfig)
            yield assertTrue(cfg.audience.isEmpty)
        },

        test("OidcConfiguration custom params include audience when set on OidcClientConfig") {
            val oidcConfiguration = new OidcConfiguration()
            val c = OidcClientConfig(
                clientId = "cid",
                clientSecret = "csec",
                discoveryURI = "https://issuer/.well-known/openid-configuration",
                audience = Some("the-api")
            )
            c.audience.foreach(a => oidcConfiguration.getCustomParams.put("audience", a))
            assertTrue(oidcConfiguration.getCustomParams.asScala.get("audience").contains("the-api"))
        },

        test("OidcConfiguration custom params do NOT include audience when unset") {
            val oidcConfiguration = new OidcConfiguration()
            val c = OidcClientConfig("cid", "csec", "https://issuer/.well-known/openid-configuration")
            c.audience.foreach(a => oidcConfiguration.getCustomParams.put("audience", a))
            assertTrue(!oidcConfiguration.getCustomParams.asScala.contains("audience"))
        }
    )

end Pac4jOidcAudienceSpec
