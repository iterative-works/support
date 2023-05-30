package works.iterative.core.service

import zio.*

trait ReadRepository[-Key, +Value]:
  type Op[A] = UIO[A]
  def find(id: Key): Op[Option[Value]]

trait WriteRepository[-Key, -Value]:
  type Op[A] = UIO[A]
  def save(key: Key, value: Value): Op[Unit]

trait Repository[-Key, Value]
    extends ReadRepository[Key, Value]
    with WriteRepository[Key, Value]
