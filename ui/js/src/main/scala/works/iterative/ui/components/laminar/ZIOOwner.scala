package works.iterative.ui.components.laminar

import zio.*
import com.raquo.airstream.ownership.Owner

object ZIOOwner:
    def acquire: UIO[ZIOOwner] = ZIO.succeed(new ZIOOwner)

    val layer: ULayer[ZIOOwner] =
        ZLayer.scoped(ZIO.acquireRelease(ZIOOwner.acquire)(_.release))
end ZIOOwner

class ZIOOwner extends Owner:
    def release: UIO[Unit] =
        ZIO.attempt(this.killSubscriptions()).ignore
