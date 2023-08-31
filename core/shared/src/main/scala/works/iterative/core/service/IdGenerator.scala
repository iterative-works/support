package works.iterative.core.service

import zio.*

trait IdGenerator[A]:
  def nextId: UIO[A]
