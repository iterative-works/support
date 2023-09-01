package works.iterative.core.service.impl

import works.iterative.core.service.IdGenerator
import zio.*

class UUIDGenerator[A](f: String => A) extends IdGenerator[A]:
  def nextId: UIO[A] = Random.nextUUID.map(v => f(v.toString))
