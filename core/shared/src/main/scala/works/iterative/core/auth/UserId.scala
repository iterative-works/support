package works.iterative.core.auth

/** Opaque type to distinguish user identifiers */
opaque type UserId = String

object UserId:
  def apply(value: String): UserId = value

  extension (userId: UserId) def value: String = userId
