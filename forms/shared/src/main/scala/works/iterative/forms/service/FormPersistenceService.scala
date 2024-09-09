package portaly.forms
package service

import zio.*
import java.time.Instant

final case class SavedForm(
    formId: String,
    formVersion: String,
    encoding: String,
    data: String,
    timestamp: Instant
)

trait FormPersistenceService:
    def put(key: FormIdent)(form: Option[SavedForm]): UIO[Unit]
    def get(key: FormIdent): UIO[Option[SavedForm]]

object FormPersistenceService:
    def put(key: FormIdent)(form: Option[SavedForm]): ZIO[FormPersistenceService, Nothing, Unit] =
        ZIO.serviceWithZIO(_.put(key)(form))
    def get(key: FormIdent): ZIO[FormPersistenceService, Nothing, Option[SavedForm]] =
        ZIO.serviceWithZIO(_.get(key))

    val empty: FormPersistenceService = new FormPersistenceService:
        def put(key: FormIdent)(form: Option[SavedForm]): UIO[Unit] = ZIO.unit
        def get(key: FormIdent): UIO[Option[SavedForm]] = ZIO.none
end FormPersistenceService
