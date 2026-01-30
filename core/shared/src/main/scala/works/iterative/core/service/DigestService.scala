package works.iterative.core.service

import works.iterative.core.Digest
import works.iterative.core.DigestAlgorithm

import zio.*
import zio.stream.*

trait DigestService:
    def digest[E](
        algorithm: DigestAlgorithm,
        content: Stream[E, Byte]
    ): IO[E, Digest]
end DigestService

object DigestService:
    def digest[E](
        algorithm: DigestAlgorithm,
        content: Stream[E, Byte]
    ): ZIO[DigestService, E, Digest] =
        ZIO.serviceWithZIO[DigestService](_.digest(algorithm, content))

    def digest(
        algorithm: DigestAlgorithm,
        content: Array[Byte]
    ): URIO[DigestService, Digest] =
        digest(algorithm, ZStream.fromChunk(Chunk.fromArray(content)))
end DigestService
