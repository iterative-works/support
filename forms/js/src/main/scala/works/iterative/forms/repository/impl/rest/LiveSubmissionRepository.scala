package works.iterative.forms.repository
package impl.rest

import zio.*
import works.iterative.tapir.ClientEndpointFactory
import portaly.forms.service.impl.rest.Endpoints

class LiveSubmissionRepository(factory: ClientEndpointFactory)
    extends ReadSubmissionRepository:
    private val loadClient = factory.make(Endpoints.submissions.load)
    private val findClient = factory.make(Endpoints.submissions.find)

    override def load(id: String): URIO[Any, Option[Submission]] =
        loadClient(id)

    override def find(filter: SubmissionRepository.Query): URIO[Any, List[Submission]] =
        findClient(filter)
end LiveSubmissionRepository

object LiveSubmissionRepository:
    val layer: URLayer[ClientEndpointFactory, ReadSubmissionRepository] =
        ZLayer {
            for factory <- ZIO.service[ClientEndpointFactory]
            yield LiveSubmissionRepository(factory)
        }
end LiveSubmissionRepository
