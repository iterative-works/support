package works.iterative.core

opaque type DigestAlgorithm = String

object DigestAlgorithm:
  val SHA256: DigestAlgorithm = "SHA-256"
  extension (d: DigestAlgorithm) def value: String = d

final case class Digest(
    algorithm: DigestAlgorithm,
    value: Array[Byte]
)

