package works.iterative.core
package service
package impl

import zio.*
import zio.stream.*

class JcaDigestService extends DigestService:

    override def digest[E](
        algorithm: DigestAlgorithm,
        content: Stream[E, Byte]
    ): IO[E, Digest] =
        for
            md <- ZIO
                .attempt(
                    java.security.MessageDigest.getInstance(algorithm.value)
                )
                .orDie
            _ <- content.runForeachChunk(e => ZIO.attempt(md.update(e.toArray)).orDie)
            digest <- ZIO.attempt(md.digest()).orDie
        yield Digest(algorithm, digest)
end JcaDigestService

object JcaDigestGenerator:
    val layer: ULayer[DigestService] =
        ZLayer.succeed(new JcaDigestService)
