package works.iterative.core.service

import zio.*
import zio.stream.*

trait GenericLoadService[Eff[+_], -Key, +Value]:
    type Op[A] = Eff[A]
    def load(id: Key): Op[Option[Value]]

trait GenericUpdateNotifyService[Str[+_], Key]:
    def updates: Str[Key]

trait GenericLoadAllService[Eff[+_], Coll[+_], -Key, +Value]:
    type Op[A] = Eff[A]
    def loadAll(ids: Seq[Key]): Op[Coll[Value]]

trait GenericFindService[Eff[+_], Coll[+_], -Key, +Value, -FilterArg]:
    type Op[A] = Eff[A]
    def find(filter: FilterArg): Op[Coll[Value]]

trait GenericReadRepository[Eff[+_], Coll[+_], -Key, +Value, -FilterArg]
    extends GenericLoadService[Eff, Key, Value]
    with GenericLoadAllService[Eff, Coll, Key, Value]
    with GenericFindService[Eff, Coll, Key, Value, FilterArg]

trait GenericWriteRepository[Eff[_], -Key, -Value]:
    type Op[A] = Eff[A]
    def save(key: Key, value: Value): Op[Unit]

trait Create[A] // Simple marker trait

trait GenericCreateRepository[Eff[+_], +Key, -Init]:
    type Op[A] = Eff[A]
    def create(value: Init): Op[Key]

trait GenericWriteRepositoryWithKeyAssignment[Eff[+_], +Key, -Value]:
    type Op[A] = Eff[A]
    def save(value: Value): Op[Key]
    def save(value: Key => Value): Op[Key]
end GenericWriteRepositoryWithKeyAssignment

trait GenericRepository[Eff[+_], -Key, Value]
    extends GenericReadRepository[Eff, Seq, Key, Value, Unit]
    with GenericWriteRepository[Eff, Key, Value]

type LoadRepository[-Key, +Value] = GenericLoadService[UIO, Key, Value]
type LoadAllRepository[-Key, +Value] =
    GenericLoadAllService[UIO, Seq, Key, Value]

trait ReadRepository[-Key, +Value, -FilterArg]
    extends GenericReadRepository[UIO, Seq, Key, Value, FilterArg]:
    override def loadAll(ids: Seq[Key]): UIO[Seq[Value]] =
        // Inefficient implementation, meant to be overridden
        ZIO.foreach(ids)(load).map(_.flatten)
end ReadRepository

trait UpdateNotifyRepository[Key]
    extends GenericUpdateNotifyService[UStream, Key]

trait WriteRepository[-Key, -Value]
    extends GenericWriteRepository[UIO, Key, Value]

trait CreateRepository[+Key, -Init]
    extends GenericCreateRepository[UIO, Key, Init]

trait WriteRepositoryWithKeyAssignment[Key, -Value]
    extends GenericWriteRepositoryWithKeyAssignment[UIO, Key, Value]

trait Repository[-Key, Value, -FilterArg]
    extends ReadRepository[Key, Value, FilterArg]
    with WriteRepository[Key, Value]

trait RepositoryWithCreate[Key, Value, -FilterArg, -Init <: Create[Value]]
    extends ReadRepository[Key, Value, FilterArg]
    with WriteRepository[Key, Value]
    with CreateRepository[Key, Init]

trait RepositoryWithKeyAssignment[Key, Value, -FilterArg]
    extends ReadRepository[Key, Value, FilterArg]
    with WriteRepositoryWithKeyAssignment[Key, Value]
