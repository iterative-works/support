package portaly.forms
package service
package impl
package rest

import works.iterative.tapir.CustomTapir.*
import sttp.capabilities.zio.ZioStreams

trait FormPersistenceApi(endpoints: FormPersistenceServiceEndpoints):
    val put: ZServerEndpoint[FormPersistenceService, ZioStreams] =
        endpoints.put.zServerLogic((key, form) =>
            FormPersistenceService.put(key)(Some(form))
        )

    val get: ZServerEndpoint[FormPersistenceService, ZioStreams] =
        endpoints.get.zServerLogic((key) =>
            FormPersistenceService.get(key)
        )

    val delete: ZServerEndpoint[FormPersistenceService, ZioStreams] =
        endpoints.delete.zServerLogic((key) =>
            FormPersistenceService.put(key)(None)
        )
end FormPersistenceApi
