package works.iterative.service.email

import zio.*

import works.iterative.core.Email

case class EmailContent(
    subject: String,
    message: String,
    attachments: Attachment*
)

case class Attachment(
    filename: String,
    mimeType: String,
    content: Array[Byte],
    description: String
)

object Attachment:
  def apply(
      filename: String,
      mimeType: String,
      content: Array[Byte]
  ): Attachment =
    Attachment(filename, mimeType, content, filename)

trait EmailNotificationService:
  type Op[A] = ZIO[Any, EmailNotificationService.Error, A]

  def sendEmail(
      subject: String,
      content: String,
      attachments: Attachment*
  )(to: Email*): Op[Unit] =
    sendEmail(EmailContent(subject, content, attachments*))(to*)

  def sendEmail(content: EmailContent)(to: Email*): Op[Unit]

object EmailNotificationService:
  // Accessors
  def sendEmail(
      subject: String,
      content: String,
      attachments: Attachment*
  )(to: Email*): ZIO[EmailNotificationService, Error, Unit] =
    ZIO.serviceWithZIO[EmailNotificationService](
      _.sendEmail(subject, content, attachments*)(to*)
    )

  def sendEmail(content: EmailContent)(to: Email*): ZIO[
    EmailNotificationService,
    Error,
    Unit
  ] =
    ZIO.serviceWithZIO[EmailNotificationService](_.sendEmail(content)(to*))

  // Errors
  sealed trait Error:
    def message: String

  object Error:
    case class ConfigurationError(message: String, ex: Option[Throwable])
        extends Error

    object ConfigurationError:
      def apply(message: String): ConfigurationError =
        ConfigurationError(message, None)

      def apply(message: String, ex: Throwable): ConfigurationError =
        ConfigurationError(message, Some(ex))

      def apply(ex: Throwable): ConfigurationError =
        ConfigurationError(ex.getMessage(), Some(ex))

    case class TransportFailed(message: String, ex: Option[Throwable])
        extends Error

    object TransportFailed:
      def apply(message: String): TransportFailed =
        TransportFailed(message, None)

      def apply(message: String, ex: Throwable): TransportFailed =
        TransportFailed(message, Some(ex))

      def apply(ex: Throwable): TransportFailed =
        TransportFailed(ex.getMessage(), Some(ex))

    case class InvalidRequest(message: String) extends Error

    object InvalidRequest:
      def apply(ex: Throwable): InvalidRequest =
        InvalidRequest(ex.getMessage())
