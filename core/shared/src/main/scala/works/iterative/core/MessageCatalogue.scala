package works.iterative
package core

// TODO: generic message catalogue
// we need to be able to render HTML messages
// like a list of items for example
trait MessageCatalogue:
  def apply(id: MessageId): String =
    get(id).getOrElse(id.toString())
  def apply(msg: UserMessage): String =
    get(msg).getOrElse(msg.id.toString())

  def get(id: MessageId): Option[String]
  def get(msg: UserMessage): Option[String]
