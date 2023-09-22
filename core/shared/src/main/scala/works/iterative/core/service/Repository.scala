package works.iterative.core.service

import zio.*

trait GenericReadRepository[Eff[+_], Coll[+_], -Key, +Value, -FilterArg]:
  type Op[A] = Eff[A]

  def load(id: Key): Op[Option[Value]]
  def loadAll(ids: Seq[Key]): Op[Coll[Value]]
  def find(filter: FilterArg): Op[Coll[Value]]

trait GenericWriteRepository[Eff[_], -Key, -Value]:
  type Op[A] = Eff[A]
  def save(key: Key, value: Value): Op[Unit]

trait GenericRepository[Eff[+_], -Key, Value]
    extends GenericReadRepository[Eff, List, Key, Value, Unit]
    with GenericWriteRepository[Eff, Key, Value]

trait ReadRepository[-Key, +Value, -FilterArg]
    extends GenericReadRepository[UIO, List, Key, Value, FilterArg]:
  override def loadAll(ids: Seq[Key]): UIO[List[Value]] =
    // Inefficient implementation, meant to be overridden
    ZIO.foreach(ids)(load).map(_.flatten.toList)

trait WriteRepository[-Key, -Value]
    extends GenericWriteRepository[UIO, Key, Value]

trait Repository[-Key, Value, -FilterArg]
    extends ReadRepository[Key, Value, FilterArg]
    with WriteRepository[Key, Value]
