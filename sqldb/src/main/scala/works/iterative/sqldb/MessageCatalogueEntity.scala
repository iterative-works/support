// PURPOSE: Domain entity representing message catalogue entries in the database
// PURPOSE: Maps between domain types (MessageId, Language) and database columns with Magnum ORM

package works.iterative.sqldb

import com.augustnagro.magnum.*
import works.iterative.core.{Language, MessageId}
import java.time.{Instant, OffsetDateTime, ZoneOffset}

given DbCodec[Instant] =
    DbCodec[OffsetDateTime].biMap(
        odt => odt.toInstant,
        instant => instant.atOffset(ZoneOffset.UTC)
    )

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class MessageCatalogueEntity(
    @Id id: Option[Long],
    messageKey: String,
    language: String,
    messageText: String,
    description: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
    createdBy: Option[String],
    updatedBy: Option[String]
) derives DbCodec

object MessageCatalogueEntity:
    def fromMessage(
        key: MessageId,
        lang: Language,
        text: String,
        desc: Option[String] = None,
        user: Option[String] = None
    ): MessageCatalogueEntity =
        val now = Instant.now()
        MessageCatalogueEntity(
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
end MessageCatalogueEntity
