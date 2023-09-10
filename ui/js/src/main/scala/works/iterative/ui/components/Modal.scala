package works.iterative.ui.components

import com.raquo.laminar.api.L.*

trait Modal:
  // TODO: this is wrong, we need to make it generic
  def open(content: HtmlElement): Unit
  def close(): Unit
