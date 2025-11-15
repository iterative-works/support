// PURPOSE: MessageCatalogue implementation backed by pre-loaded in-memory cache from SQL database
// PURPOSE: Provides pure synchronous access to messages without database queries during message lookup

package works.iterative.core
package service.impl

import scala.util.Try

/** MessageCatalogue implementation that provides synchronous access to pre-loaded messages.
  * Messages are loaded once from the database at startup and stored in an immutable Map.
  * This provides fast, pure lookups without database queries during message retrieval.
  *
  * @param language The language for this message catalogue
  * @param messages Pre-loaded map of message keys to message text
  */
class SqlMessageCatalogue(override val language: Language, messages: Map[String, String])
    extends MessageCatalogue:

  override def get(id: MessageId): Option[String] =
    messages.get(id.toString)

  override def get(msg: UserMessage): Option[String] =
    get(msg.id).map(template =>
      Try(template.format(msg.args*)).fold(
        exception => s"error formatting [${msg.id}]: '$template': ${exception.getMessage}",
        identity
      )
    )

  override val root: MessageCatalogue = this
end SqlMessageCatalogue
