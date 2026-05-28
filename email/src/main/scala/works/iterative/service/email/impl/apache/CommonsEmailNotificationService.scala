// PURPOSE: Apache Commons Email backed implementation of EmailNotificationService.
// PURPOSE: Selects MultiPartEmail or HtmlEmail depending on whether EmailContent carries an HTML alternative.

package works.iterative.service.email
package impl.apache

import zio.*
import org.apache.commons.mail.{HtmlEmail, MultiPartEmail}
import javax.mail.util.ByteArrayDataSource
import works.iterative.core.Email

class CommonsEmailNotificationService(config: SMTPConfig)
    extends EmailNotificationService:

    import EmailNotificationService.Error.*

    override def sendEmail(content: EmailContent)(to: Email*): Op[Unit] =
        for
            email <- ZIO
                .attempt(buildEmail(content))
                .mapError(InvalidRequest(_))
            _ <- ZIO
                .attempt {
                    email.setHostName(config.smtpHost)
                    email.setSmtpPort(config.smtpPort)
                    email.setFrom(config.smtpSender, config.smtpSenderName.orNull)
                    email.setStartTLSEnabled(true)
                    config.smtpUsername.foreach(
                        email.setAuthentication(_, config.smtpPassword.get)
                    )
                }
                .mapError(ConfigurationError(_))
            _ <- ZIO
                .attempt {
                    config.smtpTestRecipient match
                        case Some(recip) => email.addTo(recip)
                        case None        => to.map[String](_.value).foreach(email.addTo)
                }
                .mapError(InvalidRequest(_))
            _ <- ZIO
                .attempt(email.send())
                .mapError(TransportFailed(_))
        yield ()
        end for
    end sendEmail

    /** Build an Apache Commons email instance carrying the configured subject, body, optional HTML
      * alternative, and attachments. Returns an `HtmlEmail` (a `MultiPartEmail` subclass) when
      * `content.htmlMessage` is defined, otherwise a plain `MultiPartEmail`.
      */
    def buildEmail(content: EmailContent): MultiPartEmail =
        val email = content.htmlMessage match
            case Some(html) =>
                val htmlEmail = new HtmlEmail()
                htmlEmail.setTextMsg(content.message)
                htmlEmail.setHtmlMsg(html)
                htmlEmail
            case None =>
                val plain = new MultiPartEmail()
                plain.setMsg(content.message)
                plain
        email.setSubject(content.subject)
        content.attachments.foreach { a =>
            email.attach(
                ByteArrayDataSource(a.content, a.mimeType),
                a.filename,
                a.description
            )
        }
        email
    end buildEmail
end CommonsEmailNotificationService

object CommonsEmailNotificationService:
    val layer: TaskLayer[EmailNotificationService] =
        ZLayer {
            for config <- ZIO.config(SMTPConfig.config)
            yield CommonsEmailNotificationService(config)
        }
end CommonsEmailNotificationService
