package works.iterative
package core

import scala.annotation.tailrec

// TODO: generic message catalogue
// we need to be able to render HTML messages
// like a list of items for example
trait MessageCatalogue:

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

  @tailrec
  private def maybeResolve[T, U](id: T, fallback: T*)(
      tryResolve: T => Option[String]
  ): Option[String] =
    (tryResolve(id), fallback) match
      case (m @ Some(_), _)     => m
      case (None, next :: rest) => maybeResolve(next, rest*)(tryResolve)
      case (None, _)            => None

  private inline def resolve[T, U](id: T, fallback: T*)(
      tryResolve: T => Option[String]
  ): String =
    maybeResolve(id, fallback*)(tryResolve).getOrElse(id.toString())

  def nested(prefixes: String*): MessageCatalogue =
    NestedMessageCatalogue(this, prefixes*)

  def withPrefixes(prefixes: String*): MessageCatalogue =
    NestedMessageCatalogue(this, prefixes*)

private class NestedMessageCatalogue(
    underlying: MessageCatalogue,
    prefixes: String*
) extends MessageCatalogue:
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
