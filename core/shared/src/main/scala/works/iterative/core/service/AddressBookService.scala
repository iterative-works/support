package works.iterative.core.service

import zio.*
import works.iterative.core.auth.PermissionTarget
import works.iterative.core.Email

trait AddressBookService:
    def emailOfTarget(target: PermissionTarget): UIO[List[Email]]

object AddressBookService:
    def emailOfTarget(
        target: PermissionTarget
    ): URIO[AddressBookService, List[Email]] =
        ZIO.serviceWithZIO(_.emailOfTarget(target))
end AddressBookService
