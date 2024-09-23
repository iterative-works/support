package portaly.forms
package service

import zio.*
import works.iterative.core.Language

trait SubmissionService extends DsSubmissionService:
    // TODO: add data validation and report error back
    def submit(data: FormContent, lang: Option[Language]): UIO[SubmitResult]
    def loadPdf(id: String): UIO[Option[Chunk[Byte]]]
    def renderPdf(data: FormContent, fileName: String): UIO[Chunk[Byte]]
end SubmissionService

object SubmissionService:
    def submit(data: FormContent, lang: Option[Language]): URIO[SubmissionService, SubmitResult] =
        ZIO.serviceWithZIO(_.submit(data, lang))

    def loadPdf(id: String): URIO[SubmissionService, Option[Chunk[Byte]]] =
        ZIO.serviceWithZIO(_.loadPdf(id))

    def renderPdf(data: FormContent, fileName: String): URIO[SubmissionService, Chunk[Byte]] =
        ZIO.serviceWithZIO(_.renderPdf(data, fileName))
end SubmissionService
