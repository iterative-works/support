package works.iterative.service.email

import zio.config.*

case class SMTPConfig(
    smtpHost: String,
    smtpSender: String,
    smtpPort: Int,
    smtpUsername: Option[String],
    smtpPassword: Option[String],
    smtpTestRecipient: Option[String],
    smtpSenderName: Option[String]
)

object SMTPConfig:
    val configuration: ConfigDescriptor[SMTPConfig] =
        import ConfigDescriptor.*
        nested("SMTP")(
            string(
                "HOST"
            ) zip string("SENDER") zip int("PORT").default(25) zip string(
                "USERNAME"
            ).optional zip string(
                "PASSWORD"
            ).optional zip nested("TEST")(string("RECIPIENT").optional) zip string(
                "SENDERNAME"
            ).optional
        ).to[SMTPConfig]
    end configuration

    val fromEnv = ZConfig.fromSystemEnv(
        configuration,
        keyDelimiter = Some('_'),
        valueDelimiter = Some(',')
    )
end SMTPConfig
