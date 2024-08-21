package portaly.forms
package service
package impl
package rest

import zio.*
import works.iterative.tapir.ClientEndpointFactory

class LiveFormPersistenceService(
    factory: ClientEndpointFactory,
    endpoints: FormPersistenceServiceEndpoints
) extends FormPersistenceService:
    private val putClient = factory.make(endpoints.put)
    private val getClient = factory.make(endpoints.get)
    private val deleteClient = factory.make(endpoints.delete)

    override def put(key: FormIdent)(form: Option[SavedForm]): UIO[Unit] =
        form match
            case Some(form) => putClient((key, form))
            case None       => deleteClient(key)

    override def get(key: FormIdent): UIO[Option[SavedForm]] =
        getClient(key)
end LiveFormPersistenceService

object LiveFormPersistenceService:
    val layer
        : URLayer[ClientEndpointFactory & FormPersistenceServiceEndpoints, FormPersistenceService] =
        ZLayer.derive[LiveFormPersistenceService]
end LiveFormPersistenceService
