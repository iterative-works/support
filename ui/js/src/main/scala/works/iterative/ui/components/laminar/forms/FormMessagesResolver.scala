package works.iterative.ui.components.laminar.forms

import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.ComponentContext
import works.iterative.core.UserMessage

trait FormMessagesResolver:
  def label(name: String): String
  def help(name: String): Option[String]
  def placeholder(name: String): Option[String]
  def message(msg: UserMessage): String

object FormMessagesResolver:
  given (using ctx: ComponentContext[_]): FormMessagesResolver =
    MessageCatalogueFormMessagesResolver(using ctx.messages)

  given (using cat: MessageCatalogue): FormMessagesResolver =
    MessageCatalogueFormMessagesResolver()

class MessageCatalogueFormMessagesResolver(using cat: MessageCatalogue)
    extends FormMessagesResolver:
  override def label(name: String): String = cat(name)
  override def help(name: String): Option[String] = cat.get(s"$name.help")
  override def placeholder(name: String): Option[String] =
    cat.get(s"$name.placeholder")

  override def message(msg: UserMessage): String = cat(msg)
