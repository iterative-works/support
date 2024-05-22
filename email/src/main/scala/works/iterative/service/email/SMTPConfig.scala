package works.iterative.service.email

import zio.Config

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
    given config: Config[SMTPConfig] =
        val testConfig = Config.string("recipient").optional
        val smtpConfig =
            Config.string("host") ++ Config.string("SENDER") ++ Config.int("PORT").withDefault(
                25
            ) ++ Config.string("USERNAME").optional ++ Config.string(
                "PASSWORD"
            ).optional ++ testConfig.nested("test") ++ Config.string("sendername").optional
        smtpConfig.nested("smtp").map(SMTPConfig.apply)
    end config
end SMTPConfig
