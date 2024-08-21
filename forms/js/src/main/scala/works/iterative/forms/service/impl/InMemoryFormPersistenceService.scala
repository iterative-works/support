package portaly.forms
package service
package impl

import zio.*

class InMemoryFormPersistenceService(data: Ref[Map[FormIdent, SavedForm]])
    extends FormPersistenceService:
    override def put(id: FormIdent)(formData: Option[SavedForm]): UIO[Unit] =
        formData match
            case Some(fd) => data.update(_ + (id -> fd)).unit
            case _        => data.update(_ - id).unit

    override def get(id: FormIdent): UIO[Option[SavedForm]] =
        data.get.map(_.get(id))
end InMemoryFormPersistenceService

object InMemoryFormPersistenceService:
    val layer: ULayer[FormPersistenceService] =
        ZLayer {
            for data <- Ref.make(Map.empty[FormIdent, SavedForm])
            yield new InMemoryFormPersistenceService(data)
        }
end InMemoryFormPersistenceService
