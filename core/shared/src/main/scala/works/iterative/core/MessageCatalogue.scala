package works.iterative
package core

import scala.annotation.tailrec

// TODO: generic message catalogue
// we need to be able to render HTML messages
// like a list of items for example
trait MessageCatalogue:
  def apply(id: MessageId, fallback: MessageId*): String =
    @tailrec
    def getFirstOf(ids: List[MessageId], default: String): String =
      ids match
        case Nil => default
        case i :: is =>
          get(i) match
            case Some(m) => m
            case _       => getFirstOf(is, default)

    getFirstOf(id :: fallback.to(List), id.toString())

  def apply(msg: UserMessage, fallback: UserMessage*): String =
    @tailrec
    def getFirstOf(ids: List[UserMessage], default: String): String =
      ids match
        case Nil => default
        case i :: is =>
          get(i) match
            case Some(m) => m
            case _       => getFirstOf(is, default)

    getFirstOf(msg :: fallback.to(List), msg.id.toString())

  def opt(id: MessageId, fallback: MessageId*): Option[String] =
    @tailrec
    def getFirstOf(ids: Seq[MessageId]): Option[String] =
      ids match
        case Nil => None
        case i :: is =>
          get(i) match
            case m @ Some(_) => m
            case _           => getFirstOf(is)

    getFirstOf(id :: fallback.to(List))

  def opt(msg: UserMessage, fallback: UserMessage*): Option[String] =
    @tailrec
    def getFirstOf(ids: Seq[UserMessage]): Option[String] =
      ids match
        case Nil => None
        case i :: is =>
          get(i) match
            case m @ Some(_) => m
            case _           => getFirstOf(is)

    getFirstOf(msg :: fallback.to(List))

  def get(id: MessageId): Option[String]
  def get(msg: UserMessage): Option[String]
