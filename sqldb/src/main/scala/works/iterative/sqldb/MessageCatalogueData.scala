// PURPOSE: Data model representing a message catalogue entry for SQL storage
// PURPOSE: Contains persistence fields for database storage and audit tracking

package works.iterative.sqldb

import works.iterative.core.{Language, MessageId}
import java.time.Instant

/** Data model representing a message catalogue entry for SQL storage. Contains persistence-related
  * fields for database storage and audit tracking.
  *
  * @param messageKey
  *   The unique message identifier
  * @param language
  *   The language code for this message
  * @param messageText
  *   The actual message text content
  * @param description
  *   Optional description of the message purpose or context
  * @param createdAt
  *   Timestamp when this message was first created
  * @param updatedAt
  *   Timestamp when this message was last updated
  * @param createdBy
  *   Optional username of the user who created this message
  * @param updatedBy
  *   Optional username of the user who last updated this message
  */
case class MessageCatalogueData(
    messageKey: MessageId,
    language: Language,
    messageText: String,
    description: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
    createdBy: Option[String],
    updatedBy: Option[String]
)
