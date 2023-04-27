package works.iterative
package ui.components.tailwind

import works.iterative.core.MessageCatalogue

trait ComponentContext:
  def messages: MessageCatalogue
  def style: StyleGuide
