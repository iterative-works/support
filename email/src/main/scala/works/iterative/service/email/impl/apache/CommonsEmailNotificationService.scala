package works.iterative.service.email
package impl.apache

import zio.*
import org.apache.commons.mail.MultiPartEmail
import javax.mail.util.ByteArrayDataSource
import works.iterative.core.Email

class CommonsEmailNotificationService(config: SMTPConfig)
    extends EmailNotificationService:

    import EmailNotificationService.Error.*

    override def sendEmail(content: EmailContent)(to: Email*): Op[Unit] =
        val email = new MultiPartEmail()

        for
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
                    email.setSubject(content.subject)
                    email.setMsg(content.message)
                    content.attachments.foreach { a =>
                        email.attach(
                            ByteArrayDataSource(a.content, a.mimeType),
                            a.filename,
                            a.description
                        )
                    }
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
end CommonsEmailNotificationService

object CommonsEmailNotificationService:
    val layer: URLayer[SMTPConfig, EmailNotificationService] =
        ZLayer {
            for config <- ZIO.service[SMTPConfig]
            yield CommonsEmailNotificationService(config)
        }
end CommonsEmailNotificationService
