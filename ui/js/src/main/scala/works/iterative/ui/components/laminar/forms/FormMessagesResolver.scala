package works.iterative.ui.components.laminar.forms

import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.tailwind.ComponentContext

trait FormMessagesResolver:
  def label(name: String): String
  def help(name: String): Option[String]
  def placeholder(name: String): Option[String]

object FormMessagesResolver:
  given (using ctx: ComponentContext[_]): FormMessagesResolver =
    MessageCatalogueFormMessagesResolver(using ctx.messages)

  given (using cat: MessageCatalogue): FormMessagesResolver =
    MessageCatalogueFormMessagesResolver()

class MessageCatalogueFormMessagesResolver(using cat: MessageCatalogue)
    extends FormMessagesResolver:
  def label(name: String): String = cat(name)
  def help(name: String): Option[String] = cat.get(s"$name.help")
  def placeholder(name: String): Option[String] = cat.get(s"$name.placeholder")
