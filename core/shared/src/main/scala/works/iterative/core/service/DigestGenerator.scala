package works.iterative.core.service

import works.iterative.core.Digest
import works.iterative.core.DigestAlgorithm

import zio.*
import zio.stream.*

trait DigestGenerator:
  def generateDigest(
      algorithm: DigestAlgorithm,
      content: UStream[Byte]
  ): UIO[Digest]

object DigestGenerator:
  def generateDigest(
      algorithm: DigestAlgorithm,
      content: UStream[Byte]
  ): URIO[DigestGenerator, Digest] =
    ZIO.serviceWithZIO[DigestGenerator](_.generateDigest(algorithm, content))

  def generateDigest(
      algorithm: DigestAlgorithm,
      content: Array[Byte]
  ): URIO[DigestGenerator, Digest] =
    generateDigest(algorithm, ZStream.fromChunk(Chunk.fromArray(content)))
