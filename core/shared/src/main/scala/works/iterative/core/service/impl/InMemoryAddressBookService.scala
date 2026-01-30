package works.iterative.core.service
package impl

import zio.*
import works.iterative.core.auth.PermissionTarget
import works.iterative.core.Email

class InMemoryAddressBookService(addrBook: Map[PermissionTarget, List[Email]])
    extends AddressBookService:
    override def emailOfTarget(target: PermissionTarget): UIO[List[Email]] =
        ZIO.succeed(addrBook.getOrElse(target, List.empty))
end InMemoryAddressBookService

object InMemoryAddressBookService:
    def layer(
        addrBook: Map[PermissionTarget, List[Email]]
    ): ULayer[AddressBookService] =
        ZLayer.succeed(InMemoryAddressBookService(addrBook))
end InMemoryAddressBookService
