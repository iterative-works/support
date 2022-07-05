package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

trait StyleGuide:
  def card: Setter[HtmlElement]

object StyleGuide:
  object default extends StyleGuide:
    override def card: Setter[HtmlElement] = cls(
      "bg-white shadow px-4 py-5 sm:rounded-md sm:p-6 overflow-hidden"
    )
