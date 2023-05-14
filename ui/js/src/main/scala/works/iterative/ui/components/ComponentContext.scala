package works.iterative
package ui.components

import works.iterative.core.MessageCatalogue

trait ComponentContext[App]:
  def app: App
  def messages: MessageCatalogue

  def nested(prefixes: String*): ComponentContext[App] =
    ComponentContext.Nested[App](this, prefixes)

object ComponentContext:
  case class Nested[App](parent: ComponentContext[App], prefixes: Seq[String])
      extends ComponentContext[App]:
    export parent.{messages => _, *}

    override lazy val messages: MessageCatalogue =
      parent.messages.nested(prefixes*)
