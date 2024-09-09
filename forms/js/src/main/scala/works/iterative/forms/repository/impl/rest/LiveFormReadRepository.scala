package portaly.forms
package repository
package impl
package rest

import zio.*
import works.iterative.tapir.ClientEndpointFactory

class LiveFormReadRepository(factory: ClientEndpointFactory, endpoints: FormReadRepositoryEndpoints)
    extends FormReadRepository:
    private val loadClient = factory.make(endpoints.load)

    override def load(
        id: String,
        version: FormVersion = FormVersion.Latest
    ): URIO[Any, Option[Form]] =
        loadClient(id, version.toOption)
end LiveFormReadRepository

object LiveFormReadRepository:
    def layer(endpoints: FormReadRepositoryEndpoints)
        : URLayer[ClientEndpointFactory, FormReadRepository] =
        ZLayer {
            for factory <- ZIO.service[ClientEndpointFactory]
            yield LiveFormReadRepository(factory, endpoints)
        }
end LiveFormReadRepository
