package works.iterative.core.service

import zio.*

/** Generator of unique IDs of a given type */
trait IdGenerator[A]:
  self =>
  def nextId: UIO[A]
  def map[B](f: A => B): IdGenerator[B] = new IdGenerator[B]:
    def nextId: UIO[B] = self.nextId.map(f)
