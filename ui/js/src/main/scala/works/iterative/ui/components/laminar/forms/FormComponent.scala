package works.iterative.ui.components.laminar.forms

import zio.prelude.Validation
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.core.UserMessage
import works.iterative.core.PlainMultiLine
import com.raquo.airstream.core.Signal
import works.iterative.ui.components.tailwind.HtmlRenderable.given
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.core.MessageCatalogue

trait FormComponent[A]:
  def validated: Signal[Validated[A]]
  def element: HtmlElement
