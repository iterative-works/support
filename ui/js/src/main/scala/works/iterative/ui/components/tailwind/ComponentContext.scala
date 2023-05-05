package works.iterative
package ui.components.tailwind

import works.iterative.core.MessageCatalogue

trait ComponentContext:
  def messages: MessageCatalogue
  def style: StyleGuide

  def nested(prefixes: String*): ComponentContext =
    ComponentContext.Nested(this, prefixes)

object ComponentContext:
  case class Nested(parent: ComponentContext, prefixes: Seq[String])
      extends ComponentContext:
    export parent.{messages => _, *}

    override lazy val messages: MessageCatalogue =
      parent.messages.nested(prefixes*)
