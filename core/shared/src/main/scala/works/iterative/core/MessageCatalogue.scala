package works.iterative
package core

import scala.annotation.tailrec

// TODO: generic message catalogue
// we need to be able to render HTML messages
// like a list of items for example

/** Message catalogue for accessing localized messages by key.
  *
  * This trait provides the core interface for retrieving messages in a specific language. Messages
  * can be simple strings or templates with placeholders for formatting.
  *
  * Implementations:
  *   - [[works.iterative.ui.JsonMessageCatalogue]] - Frontend implementation using JSON
  *     dictionaries (ScalaJS)
  *   - [[works.iterative.core.service.impl.InMemoryMessageCatalogue]] - Backend implementation with
  *     pre-loaded in-memory cache (JVM)
  *
  * @see
  *   [[works.iterative.core.service.MessageCatalogueService]] for service-level access to message
  *   catalogues
  */
trait MessageCatalogue:
    // Language of this message catalogue
    def language: Language
    // These need to be implemented
    def get(id: MessageId): Option[String]
    def get(msg: UserMessage): Option[String]

    def apply(id: MessageId, fallback: => MessageId*): String =
        resolve(id, fallback*)(get(_: MessageId))

    def apply(msg: UserMessage, fallback: => UserMessage*): String =
        resolve(msg, fallback*)(get(_: UserMessage))

    def opt(id: MessageId, fallback: => MessageId*): Option[String] =
        maybeResolve(id, fallback*)(get(_: MessageId))

    def opt(msg: UserMessage, fallback: => UserMessage*): Option[String] =
        maybeResolve(msg, fallback*)(get(_: UserMessage))

    def currentPrefixes: List[String] = Nil

    def root: MessageCatalogue

    @tailrec
    private def maybeResolve[T, U](id: T, fallback: T*)(
        tryResolve: T => Option[String]
    ): Option[String] =
        (tryResolve(id), fallback) match
            case (m @ Some(_), _)     => m
            case (None, next +: rest) => maybeResolve(next, rest*)(tryResolve)
            case (None, _)            => None

    private inline def resolve[T, U](id: T, fallback: T*)(
        tryResolve: T => Option[String]
    ): String =
        maybeResolve(id, fallback*)(tryResolve).getOrElse(id.toString())

    def nested(prefixes: String*): MessageCatalogue =
        NestedMessageCatalogue(this, prefixes*)

    def withPrefixes(prefixes: String*): MessageCatalogue =
        NestedMessageCatalogue(this, prefixes*)

    // Convenience method to get the debug message catalogue
    // just by calling .debug on existing instance
    def debug: MessageCatalogue = MessageCatalogue.debug
end MessageCatalogue

object MessageCatalogue:
    /** No messages */
    val empty: MessageCatalogue = new MessageCatalogue:
        override val language: Language = Language.EN
        def get(id: MessageId): Option[String] = None
        def get(msg: UserMessage): Option[String] = None
        val root: MessageCatalogue = this

    /** Show the message keys */
    val debug: MessageCatalogue = new MessageCatalogue:
        override val language: Language = Language.EN
        def get(id: MessageId): Option[String] = Some(id.value)
        def get(msg: UserMessage): Option[String] = Some(msg.id.value)
        val root: MessageCatalogue = this
end MessageCatalogue

private class NestedMessageCatalogue(
    underlying: MessageCatalogue,
    prefixes: String*
) extends MessageCatalogue:
    export underlying.language

    // All members of MessageCatalogue, calling underlying with prefixed ids falling back to unprefixed
    def get(id: MessageId): Option[String] =
        // Iterate over the prefixes, trying to find the message, returning the first one found, trying bare id last, or None
        prefixes.view
            .map(prefix => underlying.get(s"${prefix}.${id}"))
            .collect { case Some(msg) =>
                msg
            }
            .headOption
            .orElse(underlying.get(id))

    def get(msg: UserMessage): Option[String] =
        // Iterate over the prefixes, trying to find the message, returning the first one found, trying bare id last, or None
        prefixes.view
            .map(prefix =>
                underlying.get(UserMessage(s"${prefix}.${msg.id}", msg.args*))
            )
            .collect { case Some(msg) =>
                msg
            }
            .headOption
            .orElse(underlying.get(msg))

    override def withPrefixes(prefixes: String*): MessageCatalogue =
        underlying.withPrefixes(prefixes*)

    override def currentPrefixes: List[String] =
        prefixes.toList ++ underlying.currentPrefixes

    val root: MessageCatalogue = underlying.root
end NestedMessageCatalogue
