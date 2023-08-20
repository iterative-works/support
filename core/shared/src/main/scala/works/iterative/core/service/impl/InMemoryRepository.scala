package works.iterative.core.service
package impl

import zio.*

trait InMemoryRepository[Key, Value](data: Ref[Map[Key, Value]])
    extends Repository[Key, Value]:
  override def save(key: Key, value: Value): UIO[Unit] =
    data.update(_ + (key -> value))
  override def find(key: Key): UIO[Option[Value]] =
    data.get.map(_.get(key))
