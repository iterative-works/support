package works.iterative
package core

// TODO: generic message catalogue
// we need to be able to render HTML messages
// like a list of items for example
trait MessageCatalogue:
  def apply(id: MessageId): Option[String]
  def apply(msg: UserMessage): Option[String]
