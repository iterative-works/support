// PURPOSE: MessageCatalogue implementation backed by pre-loaded in-memory cache from SQL database
// PURPOSE: Provides pure synchronous access to messages without database queries during message lookup

package works.iterative.core
package service.impl

import scala.util.Try

/** MessageCatalogue implementation backed by pre-loaded in-memory cache from SQL database.
  *
  * This implementation provides synchronous access to messages without database queries during lookup.
  * Messages are loaded once from the database (at startup or reload) and stored in an immutable Map
  * for fast O(1) lookups.
  *
  * Key characteristics:
  * - Pure synchronous access (no effects during message retrieval)
  * - O(1) lookup performance using immutable Map
  * - No database queries during message retrieval
  * - Thread-safe (immutable data structure)
  * - Formatting errors return error message instead of throwing exceptions
  *
  * The lifecycle is managed by [[SqlMessageCatalogueService]]:
  * - Pre-loaded at application startup
  * - Can be reloaded on demand via service's `reload()` method
  * - New instances created atomically on reload
  *
  * @param language The language for this message catalogue
  * @param messages Pre-loaded map of message keys to message text
  *
  * @see [[SqlMessageCatalogueService]] for service managing pre-load and reload lifecycle
  * @see [[works.iterative.core.repository.MessageCatalogueRepository]] for database access
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
