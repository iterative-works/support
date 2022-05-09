package works.iterative
package ui.components.tailwind

import com.raquo.airstream.core.Observer

trait ComponentContext:
  def eventBus: Observer[ui.model.AppEvent]
