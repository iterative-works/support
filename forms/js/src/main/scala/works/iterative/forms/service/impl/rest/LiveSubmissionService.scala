package portaly.forms
package service
package impl.rest

import zio.*
import works.iterative.tapir.ClientEndpointFactory
import works.iterative.core.Language

class LiveSubmissionService(factory: ClientEndpointFactory)
    extends SubmissionService:
    private val submitClient = factory.make(Endpoints.submissions.submit)
    private val submitDsClient =
        factory.make(Endpoints.submissions.clientSubmitDs)
    private val loadPdfClient = factory.make(Endpoints.submissions.loadPdf)
    private val renderPdfClient = factory.make(Endpoints.submissions.renderPdf)

    override def submit(data: FormContent, lang: Option[Language]): UIO[SubmitResult] =
        submitClient((data, lang))

    override def submitDs(data: FormContent): UIO[SubmitResult] =
        submitDsClient(data)

    override def loadPdf(
        id: String
    ): UIO[Option[Chunk[Byte]]] =
        loadPdfClient(id, s"${id}.pdf").map(Chunk.fromArray).map(Some(_))

    override def renderPdf(data: FormContent, fileName: String): UIO[Chunk[Byte]] =
        renderPdfClient((data, fileName)).map(Chunk.fromArray)
end LiveSubmissionService

object LiveSubmissionService:
    val layer: URLayer[ClientEndpointFactory, SubmissionService] =
        ZLayer {
            for factory <- ZIO.service[ClientEndpointFactory]
            yield LiveSubmissionService(factory)
        }
end LiveSubmissionService
