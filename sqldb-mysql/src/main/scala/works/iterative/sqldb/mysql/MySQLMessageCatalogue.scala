// PURPOSE: Database entity representing message catalogue entries in the database
// PURPOSE: Maps between domain model and database columns with Magnum ORM

package works.iterative.sqldb.mysql

import com.augustnagro.magnum.*
import works.iterative.core.{Language, MessageId}
import works.iterative.sqldb.MessageCatalogueData
import java.time.Instant
import MySQLDbCodecs.given

case class MessageCatalogueCreator(
    messageKey: String,
    language: String,
    messageText: String,
    description: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
    createdBy: Option[String],
    updatedBy: Option[String]
) derives DbCodec

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
case class MessageCatalogue(
    @Id id: Option[Long],
    messageKey: String,
    language: String,
    messageText: String,
    description: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
    createdBy: Option[String],
    updatedBy: Option[String]
) derives DbCodec:
    /** Converts this database row to domain model */
    def toDomain: MessageCatalogueData =
        MessageCatalogueData(
            messageKey = MessageId(messageKey),
            language = Language.unsafe(language),
            messageText = messageText,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy
        )
end MessageCatalogue

object MessageCatalogue:
    /** Converts domain model to database row for insertion */
    def fromDomain(data: MessageCatalogueData): MessageCatalogueCreator =
        MessageCatalogueCreator(
            messageKey = data.messageKey.value,
            language = data.language.toString,
            messageText = data.messageText,
            description = data.description,
            createdAt = data.createdAt,
            updatedAt = data.updatedAt,
            createdBy = data.createdBy,
            updatedBy = data.updatedBy
        )

    def fromMessage(
        key: MessageId,
        lang: Language,
        text: String,
        desc: Option[String] = None,
        user: Option[String] = None
    ): MessageCatalogue =
        val now = Instant.now()
        MessageCatalogue(
            id = None,
            messageKey = key.value,
            language = lang.toString,
            messageText = text,
            description = desc,
            createdAt = now,
            updatedAt = now,
            createdBy = user,
            updatedBy = user
        )

    def toCreator(entity: MessageCatalogue): MessageCatalogueCreator =
        MessageCatalogueCreator(
            messageKey = entity.messageKey,
            language = entity.language,
            messageText = entity.messageText,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy,
            updatedBy = entity.updatedBy
        )
end MessageCatalogue
