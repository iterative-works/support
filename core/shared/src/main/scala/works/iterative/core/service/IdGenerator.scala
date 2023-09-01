package works.iterative.core.service

import zio.*

/** Generator of unique IDs of a given type */
trait IdGenerator[A]:
  def nextId: UIO[A]
