package works.iterative.core
package service
package impl

import zio.*
import zio.stream.*

class JcaDigestGenerator extends DigestGenerator:

  override def generateDigest(
      algorithm: DigestAlgorithm,
      content: UStream[Byte]
  ): UIO[Digest] = {
    for
      md <- ZIO.attempt(
        java.security.MessageDigest.getInstance(algorithm.value)
      )
      _ <- content.runForeachChunk(e => ZIO.attempt(md.update(e.toArray)))
      digest <- ZIO.attempt(md.digest())
    yield Digest(algorithm, digest)
  }.orDie

object JcaDigestGenerator:
  val layer: ULayer[DigestGenerator] =
    ZLayer.succeed(new JcaDigestGenerator)
