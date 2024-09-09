package portaly
package forms
package repository

import zio.*

trait FormReadRepository:
    type Op[A] = UIO[A]

    /** Loads the specified version of the form descriptor.
      */
    def load(id: String, version: FormVersion): Op[Option[Form]]
end FormReadRepository

object FormReadRepository:
    type Op[A] = URIO[FormReadRepository, A]

    def load(ident: FormIdent): Op[Option[Form]] =
        ZIO.serviceWithZIO(_.load(ident.formId, FormVersion.Latest))

    def load(id: String, version: FormVersion): Op[Option[Form]] =
        ZIO.serviceWithZIO(_.load(id, version))
end FormReadRepository
