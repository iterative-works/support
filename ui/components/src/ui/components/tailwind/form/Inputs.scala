package works.iterative
package ui.components.tailwind.form

import com.raquo.laminar.api.L.{textArea => ta, *, given}
import works.iterative.ui.components.tailwind.HtmlComponent
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.model.Paragraph
import works.iterative.ui.model.FormItem
import org.scalajs.dom.html

object Inputs:

  def textArea(
      updates: Observer[Option[Paragraph]]
  ): HtmlComponent[html.TextArea, FormItem[Paragraph]] =
    (i: FormItem[Paragraph]) =>
      ta(
        idAttr := i.id,
        name := i.id,
        rows := 5,
        cls := "max-w-lg shadow-sm block w-full focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md",
        value(i.value.toString),
        onInput.mapToValue.setAsValue --> updates.contramap((v: String) =>
          Option(v).map(_.trim).filter(_.nonEmpty).map(Paragraph(_))
        )
      )
