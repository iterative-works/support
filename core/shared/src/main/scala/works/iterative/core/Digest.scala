package works.iterative.core

import works.iterative.core.service.DigestService

import zio.*

opaque type DigestAlgorithm = String

object DigestAlgorithm:
  val SHA256: DigestAlgorithm = "SHA-256"
  extension (d: DigestAlgorithm) def value: String = d

final case class Digest(
    algorithm: DigestAlgorithm,
    value: Array[Byte]
)

object Digest:
  def compute(
      algorithm: DigestAlgorithm,
      value: Array[Byte]
  ): URIO[DigestService, Digest] =
    DigestService.digest(algorithm, value)
