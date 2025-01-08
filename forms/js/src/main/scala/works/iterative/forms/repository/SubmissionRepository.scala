package works.iterative.forms
package repository

import zio.*
import works.iterative.core.service.ReadRepository
import works.iterative.core.service.Repository
import java.time.OffsetDateTime
import portaly.forms.FormContent

trait ReadSubmissionRepository
    extends ReadRepository[String, Submission, SubmissionRepository.Query]
trait SubmissionRepository extends ReadSubmissionRepository
    with Repository[String, Submission, SubmissionRepository.Query]:
    def create(data: FormContent, date: OffsetDateTime, zavazna: Boolean): UIO[String]
    def updatePaymentInfo(
        id: String,
        paymentId: Option[String],
        paymentUrl: Option[String]
    ): UIO[Unit]
    def updatePaymentStatus(paymentId: String, paymentResult: Boolean): UIO[Unit]
    def updateStrediska(id: String, strediska: Option[List[String]]): UIO[Unit]
    def updateStav(id: String, stav: Option[String]): UIO[Unit]
end SubmissionRepository

object SubmissionRepository:
    final case class Query(
        ic: Option[String] = None,
        paymentId: Option[String] = None
    )

    def load(id: String): URIO[SubmissionRepository, Option[Submission]] =
        ZIO.serviceWithZIO(_.load(id))

    def find(filter: Query): URIO[SubmissionRepository, List[Submission]] =
        ZIO.serviceWithZIO(_.find(filter))

    def save(id: String, value: Submission): URIO[SubmissionRepository, Unit] =
        ZIO.serviceWithZIO(_.save(id, value))

    def create(
        data: FormContent,
        date: OffsetDateTime,
        zavazna: Boolean
    ): URIO[SubmissionRepository, String] =
        ZIO.serviceWithZIO(_.create(data, date, zavazna))

    def updatePaymentUrl(
        id: String,
        paymentId: Option[String],
        paymentUrl: Option[String]
    ): URIO[SubmissionRepository, Unit] =
        ZIO.serviceWithZIO(_.updatePaymentInfo(id, paymentId, paymentUrl))

    def updatePaymentStatus(
        paymentId: String,
        paymentResult: Boolean
    ): URIO[SubmissionRepository, Unit] =
        ZIO.serviceWithZIO(_.updatePaymentStatus(paymentId, paymentResult))

    def updateStrediska(
        id: String,
        strediska: Option[List[String]]
    ): URIO[SubmissionRepository, Unit] =
        ZIO.serviceWithZIO(_.updateStrediska(id, strediska))

    def updateStav(
        id: String,
        stav: Option[String]
    ): URIO[SubmissionRepository, Unit] =
        ZIO.serviceWithZIO(_.updateStav(id, stav))
end SubmissionRepository
