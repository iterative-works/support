package works.iterative
package ui.components.tailwind

import com.raquo.airstream.core.Observer
import works.iterative.core.MessageCatalogue

trait ComponentContext:
  def eventBus: Observer[ui.model.AppEvent]
  def messages: MessageCatalogue
