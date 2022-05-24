package works.iterative
package core

trait MessageCatalogue:
  def apply(id: MessageId): Option[String]
  def apply(msg: UserMessage): Option[String]
