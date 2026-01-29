package works.iterative.core.service
package impl

import zio.*

trait InMemoryRepository[Key, Value, FilterArg](
    data: Ref[Map[Key, Value]],
    filter: FilterArg => Value => Boolean
) extends Repository[Key, Value, FilterArg]:
    override def save(key: Key, value: Value): UIO[Unit] =
        data.update(_ + (key -> value))
    override def load(key: Key): UIO[Option[Value]] =
        data.get.map(_.get(key))
    override def loadAll(keys: Seq[Key]): UIO[List[Value]] =
        data.get.map(_.view.filterKeys(keys.contains).values.toList)
    override def find(filterArg: FilterArg): UIO[List[Value]] =
        data.get.map(_.values.filter(filter(filterArg)).toList)
end InMemoryRepository
