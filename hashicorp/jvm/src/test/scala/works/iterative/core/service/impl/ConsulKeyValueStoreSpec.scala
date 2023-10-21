package works.iterative.core
package service
package impl

import works.iterative.tapir.{BaseUri, CustomTapir}
import zio.*
import zio.test.*

object ConsulKeyValueStoreSpec extends specs.KeyValueStoreSpec:
  override def spec = defaultSpec.provide(
    ConsulKeyValueStore.layer,
    CustomTapir.clientLayer,
    ZLayer {
      Live.live(System.env("CONSUL_ADDR")).map(a => BaseUri(a.get))
    },
    ZLayer {
      Live.live(System.env("CONSUL_TOKEN")).map(ConsulToken(_))
    }
  ) @@ TestAspect.ifEnvSet("CONSUL_ADDR")
