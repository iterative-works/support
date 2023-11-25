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

trait GenericRepository[Eff[+_], -Key, Value]
    extends GenericReadRepository[Eff, List, Key, Value, Unit]
    with GenericWriteRepository[Eff, Key, Value]

type LoadRepository[-Key, +Value] = GenericLoadService[UIO, Key, Value]
type LoadAllRepository[-Key, +Value] =
    GenericLoadAllService[UIO, List, Key, Value]

trait ReadRepository[-Key, +Value, -FilterArg]
    extends GenericReadRepository[UIO, List, Key, Value, FilterArg]:
    override def loadAll(ids: Seq[Key]): UIO[List[Value]] =
        // Inefficient implementation, meant to be overridden
        ZIO.foreach(ids)(load).map(_.flatten.toList)
end ReadRepository

trait UpdateNotifyRepository[Key]
    extends GenericUpdateNotifyService[UStream, Key]

trait WriteRepository[-Key, -Value]
    extends GenericWriteRepository[UIO, Key, Value]

trait Repository[-Key, Value, -FilterArg]
    extends ReadRepository[Key, Value, FilterArg]
    with WriteRepository[Key, Value]
